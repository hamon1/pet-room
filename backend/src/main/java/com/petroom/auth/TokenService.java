package com.petroom.auth;

import com.petroom.global.config.JwtProperties;
import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 액세스 토큰(JWT, 짧은 수명)과 리프레시 토큰(무작위 문자열, 긴 수명)을 발급합니다.
 * 리프레시 토큰은 사용할 때마다 새로 바꾸고(회전), 이미 쓴 토큰이 다시 오면 탈취로 보고 그 사용자의 토큰을 모두 무효화합니다.
 */
@Service
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties,
                        RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Transactional
    public TokenResponse issue(Long userId) {
        Instant now = clock.instant();
        String refreshToken = newRefreshToken();
        refreshTokenRepository.save(new RefreshToken(userId, hash(refreshToken),
                now.plusSeconds(properties.refreshExpiresInSeconds())));
        return new TokenResponse(accessToken(userId, now), "Bearer", properties.expiresInSeconds(),
                refreshToken, properties.refreshExpiresInSeconds());
    }

    /** 실패해도 재사용 감지로 무효화한 내용은 저장되어야 하므로 BusinessException에서 롤백하지 않습니다. */
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse rotate(String refreshToken) {
        Instant now = clock.instant();
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(stored.getUserId(), now);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (stored.isExpired(now)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        stored.revoke(now);
        return issue(stored.getUserId());
    }

    @Transactional
    public void revoke(String refreshToken) {
        refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    private String accessToken(Long userId, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(properties.expiresInSeconds()))
                .subject(String.valueOf(userId))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

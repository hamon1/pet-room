package com.petroom.auth.oauth;

import com.petroom.auth.TokenResponse;
import com.petroom.auth.TokenService;
import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import com.petroom.user.User;
import com.petroom.user.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인. 처음 들어온 계정이면 새 사용자를 만듭니다.
 * 같은 이메일의 기존 계정과 자동으로 합치지 않습니다(이메일만으로 남의 계정에 들어가는 것을 막기 위해).
 */
@Service
public class SocialLoginService {

    private static final Logger log = LoggerFactory.getLogger(SocialLoginService.class);
    private static final String DEFAULT_NICKNAME = "새 보호자";
    private static final int NICKNAME_MAX = 20;

    private final OAuthClients clients;
    private final OAuthProperties properties;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public SocialLoginService(OAuthClients clients, OAuthProperties properties,
                              SocialAccountRepository socialAccountRepository, UserRepository userRepository,
                              TokenService tokenService) {
        this.clients = clients;
        this.properties = properties;
        this.socialAccountRepository = socialAccountRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public TokenResponse login(String providerPath, OAuthLoginRequest request) {
        OAuthProvider provider = OAuthProvider.fromPath(providerPath);
        OAuthClient client = clients.get(provider);
        if (properties.allowedRedirectUris() == null || !properties.allowedRedirectUris().contains(request.redirectUri())) {
            throw new BusinessException(ErrorCode.INVALID_REDIRECT_URI);
        }

        OAuthProfile profile;
        try {
            profile = client.fetchProfile(request.code(), request.redirectUri());
        } catch (RuntimeException e) {
            // 만료·재사용된 인가 코드, 제공자 장애 등. 원인은 서버 로그로만 남깁니다.
            log.warn("OAuth login failed for {}: {}", provider, e.getMessage());
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }

        Long userId = socialAccountRepository.findByProviderAndProviderUserId(provider, profile.providerUserId())
                .map(SocialAccount::getUserId)
                .orElseGet(() -> register(provider, profile));
        return tokenService.issue(userId);
    }

    private Long register(OAuthProvider provider, OAuthProfile profile) {
        String email = null;
        if (profile.email() != null && profile.emailVerified()) {
            String normalized = profile.email().strip().toLowerCase(Locale.ROOT);
            if (!userRepository.existsByEmail(normalized)) {
                email = normalized;
            }
        }
        User user = userRepository.save(User.createSocial(email, nickname(profile.nickname())));
        socialAccountRepository.save(new SocialAccount(user.getId(), provider, profile.providerUserId()));
        return user.getId();
    }

    private static String nickname(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_NICKNAME;
        }
        String trimmed = raw.strip();
        return trimmed.length() > NICKNAME_MAX ? trimmed.substring(0, NICKNAME_MAX) : trimmed;
    }
}

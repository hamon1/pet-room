package com.petroom.push;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPrivateKey;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;

/** VAPID 인증 헤더 (RFC 8292): 푸시 서비스에 "이 서버가 보낸 알림"임을 증명합니다. */
final class VapidSigner {

    private static final Duration TTL = Duration.ofHours(12);

    private final ECPrivateKey privateKey;
    private final String publicKey;
    private final String subject;
    private final Clock clock;

    VapidSigner(PushProperties properties, Clock clock) throws GeneralSecurityException {
        this.privateKey = EcKeys.privateKey(EcKeys.b64(properties.vapidPrivateKey()));
        EcKeys.publicKey(EcKeys.b64(properties.vapidPublicKey())); // 형식 확인
        this.publicKey = properties.vapidPublicKey().strip();
        this.subject = properties.subject().strip();
        this.clock = clock;
    }

    String authorization(String endpoint) {
        URI uri = URI.create(endpoint);
        String audience = uri.getScheme() + "://" + uri.getAuthority();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience(audience)
                .expirationTime(Date.from(clock.instant().plus(TTL)))
                .subject(subject)
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build(), claims);
        try {
            jwt.sign(new ECDSASigner(privateKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("VAPID signing failed", e);
        }
        return "vapid t=" + jwt.serialize() + ", k=" + publicKey;
    }
}

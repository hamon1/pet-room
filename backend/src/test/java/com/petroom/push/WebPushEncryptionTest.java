package com.petroom.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebPushEncryptionTest {

    static KeyPair p256() throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(EcKeys.P256);
        return gen.generateKeyPair();
    }

    /** 브라우저 쪽 복호화 (RFC 8291 수신 측 절차) */
    static byte[] decrypt(byte[] body, KeyPair ua, byte[] auth) throws GeneralSecurityException {
        byte[] salt = Arrays.copyOfRange(body, 0, 16);
        int rs = ByteBuffer.wrap(body, 16, 4).getInt();
        int idLen = body[20] & 0xff;
        byte[] asPublic = Arrays.copyOfRange(body, 21, 21 + idLen);
        byte[] ciphertext = Arrays.copyOfRange(body, 21 + idLen, body.length);
        assertThat(rs).isEqualTo(4096);

        byte[] uaPublic = EcKeys.uncompressed((ECPublicKey) ua.getPublic());
        byte[] ecdh = WebPushEncryption.ecdh((ECPrivateKey) ua.getPrivate(), EcKeys.publicKey(asPublic));
        WebPushEncryption.Keys k = WebPushEncryption.derive(ecdh, auth, uaPublic, asPublic, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(k.cek(), "AES"), new GCMParameterSpec(128, k.nonce()));
        byte[] plain = cipher.doFinal(ciphertext);
        assertThat(plain[plain.length - 1]).as("last record delimiter").isEqualTo((byte) 2);
        return Arrays.copyOf(plain, plain.length - 1);
    }

    @Test
    @DisplayName("암호화한 알림을 구독한 브라우저의 키로 복호화하면 원문이 나온다")
    void roundTrip() throws Exception {
        KeyPair ua = p256();
        byte[] auth = new byte[16];
        new SecureRandom().nextBytes(auth);
        byte[] payload = "{\"title\":\"오늘 챙길 일 2건\"}".getBytes(StandardCharsets.UTF_8);

        byte[] body = WebPushEncryption.encrypt(payload, EcKeys.uncompressed((ECPublicKey) ua.getPublic()), auth);

        assertThat(body.length).isEqualTo(16 + 4 + 1 + 65 + payload.length + 1 + 16);
        assertThat(decrypt(body, ua, auth)).isEqualTo(payload);
    }

    @Test
    @DisplayName("다른 auth 값으로는 복호화되지 않는다")
    void wrongAuthFails() throws Exception {
        KeyPair ua = p256();
        byte[] body = WebPushEncryption.encrypt("hi".getBytes(StandardCharsets.UTF_8),
                EcKeys.uncompressed((ECPublicKey) ua.getPublic()), new byte[16]);
        byte[] other = new byte[16];
        other[0] = 1;
        assertThatThrownBy(() -> decrypt(body, ua, other)).isInstanceOf(GeneralSecurityException.class);
    }

    @Test
    @DisplayName("키 변환: 공개키 65바이트, 개인키 32바이트 형식을 주고받을 수 있다")
    void keyCodec() throws Exception {
        KeyPair kp = p256();
        byte[] pub = EcKeys.uncompressed((ECPublicKey) kp.getPublic());
        byte[] priv = EcKeys.raw((ECPrivateKey) kp.getPrivate());
        assertThat(pub).hasSize(65);
        assertThat(EcKeys.publicKey(pub).getW()).isEqualTo(((ECPublicKey) kp.getPublic()).getW());
        assertThat(EcKeys.privateKey(priv).getS()).isEqualTo(((ECPrivateKey) kp.getPrivate()).getS());
    }

    @Test
    @DisplayName("VAPID 헤더: endpoint의 origin을 aud로 서명하고 공개키로 검증된다")
    void vapid() throws Exception {
        KeyPair kp = p256();
        String pub = EcKeys.b64(EcKeys.uncompressed((ECPublicKey) kp.getPublic()));
        String priv = EcKeys.b64(EcKeys.raw((ECPrivateKey) kp.getPrivate()));
        Clock clock = Clock.fixed(Instant.parse("2026-11-04T00:00:00Z"), ZoneOffset.UTC);
        VapidSigner signer = new VapidSigner(new PushProperties(pub, priv, "mailto:ops@example.com", "0 0 9 * * *"), clock);

        String header = signer.authorization("https://fcm.googleapis.com/fcm/send/abc123");

        assertThat(header).startsWith("vapid t=").endsWith(", k=" + pub);
        SignedJWT jwt = SignedJWT.parse(header.substring("vapid t=".length(), header.indexOf(',')));
        assertThat(jwt.verify(new ECDSAVerifier((ECPublicKey) kp.getPublic()))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("https://fcm.googleapis.com");
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("mailto:ops@example.com");
        assertThat(jwt.getJWTClaimsSet().getExpirationTime().toInstant()).isEqualTo(Instant.parse("2026-11-04T12:00:00Z"));
    }

    @Test
    @DisplayName("알려진 브라우저 푸시 서비스 주소만 허용한다 (SSRF 방지)")
    void endpointAllowList() {
        assertThat(PushService.allowedEndpoint("https://fcm.googleapis.com/fcm/send/x")).isTrue();
        assertThat(PushService.allowedEndpoint("https://updates.push.services.mozilla.com/wpush/v2/x")).isTrue();
        assertThat(PushService.allowedEndpoint("https://web.push.apple.com/x")).isTrue();
        assertThat(PushService.allowedEndpoint("http://fcm.googleapis.com/x")).isFalse();
        assertThat(PushService.allowedEndpoint("https://169.254.169.254/latest")).isFalse();
        assertThat(PushService.allowedEndpoint("https://evil-fcm.googleapis.com.attacker.io/x")).isFalse();
        assertThat(PushService.allowedEndpoint("https://user@fcm.googleapis.com/x")).isFalse();
    }
}

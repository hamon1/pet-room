package com.petroom.push;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 웹 푸시 메시지 암호화 (RFC 8291, Content-Encoding: aes128gcm, RFC 8188).
 * 푸시 서비스는 내용을 볼 수 없고, 구독한 브라우저만 복호화할 수 있습니다.
 */
final class WebPushEncryption {

    static final int RECORD_SIZE = 4096;
    private static final SecureRandom RANDOM = new SecureRandom();

    private WebPushEncryption() {
    }

    /**
     * @param uaPublic   브라우저 공개키 (구독의 p256dh, 65바이트)
     * @param authSecret 구독의 auth (16바이트)
     */
    static byte[] encrypt(byte[] payload, byte[] uaPublic, byte[] authSecret) throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(EcKeys.P256);
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return encrypt(payload, uaPublic, authSecret, gen.generateKeyPair(), salt);
    }

    /** 테스트용: 서버 임시 키쌍과 salt를 지정 */
    static byte[] encrypt(byte[] payload, byte[] uaPublic, byte[] authSecret, KeyPair asKeys, byte[] salt)
            throws GeneralSecurityException {
        if (payload.length > RECORD_SIZE - 17 - 86) {
            throw new GeneralSecurityException("payload too large for a single record");
        }
        byte[] asPublic = EcKeys.uncompressed((ECPublicKey) asKeys.getPublic());
        byte[] ecdh = ecdh((ECPrivateKey) asKeys.getPrivate(), EcKeys.publicKey(uaPublic));
        Keys k = derive(ecdh, authSecret, uaPublic, asPublic, salt);

        // 마지막 레코드 구분자 0x02, 추가 패딩 없음
        byte[] plain = Arrays.copyOf(payload, payload.length + 1);
        plain[payload.length] = 0x02;
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k.cek, "AES"), new GCMParameterSpec(128, k.nonce));
        byte[] ciphertext = cipher.doFinal(plain);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(salt);
        out.writeBytes(ByteBuffer.allocate(4).putInt(RECORD_SIZE).array());
        out.write(asPublic.length);
        out.writeBytes(asPublic);
        out.writeBytes(ciphertext);
        return out.toByteArray();
    }

    record Keys(byte[] cek, byte[] nonce) {
    }

    static Keys derive(byte[] ecdh, byte[] authSecret, byte[] uaPublic, byte[] asPublic, byte[] salt)
            throws GeneralSecurityException {
        byte[] prkKey = hmac(authSecret, ecdh);
        byte[] keyInfo = concat("WebPush: info".getBytes(StandardCharsets.US_ASCII), new byte[] {0}, uaPublic, asPublic);
        byte[] ikm = expand(prkKey, keyInfo, 32);
        byte[] prk = hmac(salt, ikm);
        byte[] cek = expand(prk, concat("Content-Encoding: aes128gcm".getBytes(StandardCharsets.US_ASCII), new byte[] {0}), 16);
        byte[] nonce = expand(prk, concat("Content-Encoding: nonce".getBytes(StandardCharsets.US_ASCII), new byte[] {0}), 12);
        return new Keys(cek, nonce);
    }

    static byte[] ecdh(ECPrivateKey priv, ECPublicKey pub) throws GeneralSecurityException {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(priv);
        ka.doPhase(pub, true);
        return ka.generateSecret();
    }

    /** HKDF-Expand, 출력이 32바이트 이하인 경우 (한 블록) */
    private static byte[] expand(byte[] prk, byte[] info, int length) throws GeneralSecurityException {
        return Arrays.copyOf(hmac(prk, concat(info, new byte[] {1})), length);
    }

    private static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.writeBytes(p);
        }
        return out.toByteArray();
    }
}

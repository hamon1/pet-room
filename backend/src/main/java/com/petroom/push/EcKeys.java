package com.petroom.push;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;

/** 웹 푸시에서 쓰는 P-256 키와 base64url 변환 */
final class EcKeys {

    static final ECParameterSpec P256 = p256();

    private EcKeys() {
    }

    static byte[] b64(String value) {
        return Base64.getUrlDecoder().decode(value.strip());
    }

    static String b64(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    /** 압축하지 않은 점 형식(0x04 || X || Y, 65바이트)을 공개키로 */
    static ECPublicKey publicKey(byte[] uncompressed) throws GeneralSecurityException {
        if (uncompressed.length != 65 || uncompressed[0] != 0x04) {
            throw new GeneralSecurityException("P-256 public key must be 65 bytes uncompressed");
        }
        BigInteger x = new BigInteger(1, java.util.Arrays.copyOfRange(uncompressed, 1, 33));
        BigInteger y = new BigInteger(1, java.util.Arrays.copyOfRange(uncompressed, 33, 65));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
    }

    static ECPrivateKey privateKey(byte[] raw) throws GeneralSecurityException {
        if (raw.length != 32) {
            throw new GeneralSecurityException("P-256 private key must be 32 bytes");
        }
        return (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, raw), P256));
    }

    static byte[] uncompressed(ECPublicKey key) {
        byte[] out = new byte[65];
        out[0] = 0x04;
        copyFixed(key.getW().getAffineX(), out, 1);
        copyFixed(key.getW().getAffineY(), out, 33);
        return out;
    }

    static byte[] raw(ECPrivateKey key) {
        byte[] out = new byte[32];
        copyFixed(key.getS(), out, 0);
        return out;
    }

    private static void copyFixed(BigInteger value, byte[] dest, int offset) {
        byte[] bytes = value.toByteArray();
        int start = Math.max(0, bytes.length - 32);
        int len = bytes.length - start;
        System.arraycopy(bytes, start, dest, offset + 32 - len, len);
    }

    private static ECParameterSpec p256() {
        try {
            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec("secp256r1"));
            return params.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}

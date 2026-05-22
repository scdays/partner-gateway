package com.vtc.openplatform.gateway.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 十六进制工具（避免在循环中使用 {@link String#format}）。
 */
public final class Sha256Utils {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private static final String ALGORITHM_SHA_256 = "SHA-256";

    private Sha256Utils() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    public static String hex(String plainText) {
        if (plainText == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM_SHA_256);
            byte[] hash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ALGORITHM_SHA_256 + " not available", ex);
        }
    }

    private static String toHex(byte[] hash) {
        char[] chars = new char[hash.length * 2];
        for (int i = 0; i < hash.length; i++) {
            int value = hash[i] & 0xFF;
            chars[i * 2] = HEX_DIGITS[value >>> 4];
            chars[i * 2 + 1] = HEX_DIGITS[value & 0x0F];
        }
        return new String(chars);
    }
}

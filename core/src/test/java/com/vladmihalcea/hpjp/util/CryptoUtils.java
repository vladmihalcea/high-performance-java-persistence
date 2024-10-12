package com.vladmihalcea.hpjp.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;

/**
 * @author  Vlad Mihalcea
 */
public final class CryptoUtils {

    private static String ENCODING = "UTF-8";

    private static byte[] ENCRYPT_KEY_BYTES = new byte[] {
        48, -126, 1, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48, -126, 1, 10, 2, -126, 1, 1, 0, -33, 58, -60, -67, 82, -114, 43, -3, -15, 74, -69, -14, 123, -51, 29, -33, -106, -94, -115, -57, 52, -124, 106, -56, 55, 0, -98, -56, -124, -106, -122, 101, 117, -49, 96, 126, 4, -3, -91, -31, -100, -42, 10, 103, -93, -128, -82, 34, 63, 33, 48, -69, 45, 121, 33, 99, -50, 18, -119, -102, -24, 122, -103, 107, 124, -16, 34, 83, -30, -51, -54, -38, -50, -82, 86, 101, -9, -72, 28, 42, 66, 14, 76, -107, -91, -53, -30, 21, 80, 109, -1, -41, -61, -69, 39, 87, -17, 35, 48, 51, -58, -91, -109, 29, -18, -54, 104, -30, -114, -120, -10, 11, -47, 35, -112, -121, 54, 20, -47, 127, 39, 76, -86, 1, -71, 64, -56, -49, -113, 65, 120, -67, 59, 126, 25, -71, -24, -63, -33, 36, -44, 110, -14, 46, -120, 73, 55, -86, -110, 98, -71, -124, -67, 17, -37, -122, 68, -36, 116, -65, -32, 8, 104, -17, -65, 96, 85, -16, -7, 24, 19, -91, 38, 111, 91, -17, 39, -9, 89, -95, -54, -38, -20, 113, 82, 64, -24, -114, 8, 72, -96, -79, 116, -12, 63, 61, 59, 119, 28, -98, 86, -55, -99, 12, 123, 17, -29, -35, 3, -118, -120, -87, 4, 123, -46, -28, 15, -54, -26, -81, -47, 40, 28, 109, 98, 78, 16, 113, -11, -59, 82, 34, 41, 69, 54, 16, -24, 89, -95, -40, 58, 32, 72, 124, 13, 21, -34, 24, -33, -66, 89, 74, 21, 38, 118, 27, 2, 3, 1, 0, 1
    };

    public static SecretKeySpec getEncryptionKey() {
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(ENCRYPT_KEY_BYTES);
            key = Arrays.copyOf(key, 16);
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String encrypt(Object message) {
        if(message == null) {
            throw new IllegalArgumentException("Only not-null values can be encrypted!");
        }
        try {
            Cipher cipher = getCipher();
            cipher.init(Cipher.ENCRYPT_MODE, getEncryptionKey());
            String messageValue = (message instanceof String) ?
                (String) message :
                String.valueOf(message);
            return Base64.getEncoder().encodeToString(
                cipher.doFinal(messageValue.getBytes(ENCODING))
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(String message) {
        try {
            Cipher cipher = getCipher();
            cipher.init(Cipher.DECRYPT_MODE, getEncryptionKey());
            return new String(cipher.doFinal(Base64.getDecoder().decode(message)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T decrypt(String message, Class<T> clazz) {
        try {
            Cipher cipher = getCipher();
            cipher.init(Cipher.DECRYPT_MODE, getEncryptionKey());
            String decryptedValue = new String(cipher.doFinal(Base64.getDecoder().decode(message)));
            if (String.class.equals(clazz)) {
                return (T) decryptedValue;
            } else {
                return ReflectionUtils.invokeStaticMethod(
                    ReflectionUtils.getMethodOrNull(clazz, "valueOf", String.class),
                    decryptedValue
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Cipher getCipher() {
        try {
            return Cipher.getInstance("AES/ECB/PKCS5PADDING");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

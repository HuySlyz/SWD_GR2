package com.warranty.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Bam mat khau PBKDF2-HmacSHA256 (co san trong JDK, khong can thu vien ngoai).
 * Dinh dang luu trong users.password_hash: pbkdf2$vong_lap$salt_base64$hash_base64
 */
public final class PasswordUtil {

    private static final int ITERATIONS = 65_536;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] dk = derive(password.toCharArray(), salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(dk);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] p = stored.split("\\$");
        if (p.length != 4 || !"pbkdf2".equals(p[0])) {
            return false;
        }
        Base64.Decoder b64 = Base64.getDecoder();
        byte[] expected = b64.decode(p[3]);
        byte[] actual = derive(password.toCharArray(), b64.decode(p[2]), Integer.parseInt(p[1]));
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Chay rieng de sinh hash cho du lieu mau: java PasswordUtil.java <mat_khau> */
    public static void main(String[] args) {
        System.out.println(hash(args.length > 0 ? args[0] : "123456"));
    }
}

package com.ahmed2027.app.friendkey;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Pure PBKDF2 verification, constant-time comparison, input array zeroed. No Android deps. */
public final class KeyVerifier {
    private KeyVerifier() {}

    /** Derives with PBKDF2WithHmacSHA256. Caller owns (and should wipe) the returned bytes. */
    public static byte[] derive(char[] password, byte[] salt, int iterations, int keyBytes)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBytes * 8);
        try {
            return SecretKeyFactory.getInstance(FriendKeyConfig.ALGO).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /** @return true when the derived key equals expected. Always zeroes {@code input}. */
    public static boolean verify(char[] input, byte[] salt, int iterations, byte[] expected) {
        byte[] got = null;
        try {
            if (input == null || input.length == 0) return false;
            got = derive(input, salt, iterations, expected.length);
            return MessageDigest.isEqual(got, expected); // constant-time
        } catch (GeneralSecurityException | RuntimeException e) {
            return false;
        } finally {
            if (input != null) Arrays.fill(input, '\0');
            if (got != null) Arrays.fill(got, (byte) 0);
        }
    }
}

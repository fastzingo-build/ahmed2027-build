package com.ahmed2027.app.friendkey;

import java.util.Base64;

/** Entry point used by the UI and the Shield. Call off the main thread (310k PBKDF2 rounds). */
public final class FriendKey {
    private FriendKey() {}

    /** Verifies the pasted key against the stored hash and zeroes the array. */
    public static boolean verify(char[] input) {
        byte[] salt = Base64.getDecoder().decode(FriendKeyConfig.SALT_B64);
        byte[] hash = Base64.getDecoder().decode(FriendKeyConfig.HASH_B64);
        return KeyVerifier.verify(input, salt, FriendKeyConfig.ITERATIONS, hash);
    }
}

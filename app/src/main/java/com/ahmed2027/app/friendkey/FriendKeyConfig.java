package com.ahmed2027.app.friendkey;

/**
 * Friend Key verification parameters. The key itself is NOT stored anywhere in this project,
 * only its salted PBKDF2 hash. To rotate the key run tools/regen_friend_key.py and paste the
 * four printed values here.
 */
public final class FriendKeyConfig {
    public static final String ALGO = "PBKDF2WithHmacSHA256";
    public static final int ITERATIONS = 310000;
    public static final String SALT_B64 = "2HUstrpmJv47kdasZKf5iA==";
    public static final String HASH_B64 = "gpCZ5DwUKXfFnxN783x7nu97pGO683mybmMD6f/VFz8=";
    public static final int KEY_BYTES = 32;

    private FriendKeyConfig() {}
}

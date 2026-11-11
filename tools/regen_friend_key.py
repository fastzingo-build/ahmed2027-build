#!/usr/bin/env python3
"""Generate a NEW 48-char Friend Key and the 4 constants for FriendKeyConfig.java.

Run it on your own machine (Termux is fine). It writes the key to friend_key.txt in the
CURRENT directory (give that text to your friend ONLY, then delete the file) and prints
the algo / iterations / salt / hash to paste into friendkey/FriendKeyConfig.java.
The project never contains the key, only its hash. Never paste the key into any chat.
"""
import base64, hashlib, os, secrets, sys

ITER = 310000
ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"  # no look-alikes
key = "".join(secrets.choice(ALPHABET) for _ in range(48))
salt = os.urandom(16)
dk = hashlib.pbkdf2_hmac("sha256", key.encode("utf-8"), salt, ITER, dklen=32)
out = "friend_key.txt"
fd = os.open(out, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, "w") as f:
    f.write(key + "\n")
print("Key written to ./%s (mode 600). Send it to your friend, then delete the file." % out)
print("Paste into FriendKeyConfig.java:")
print('  ALGO       = "PBKDF2WithHmacSHA256"')
print("  ITERATIONS = %d" % ITER)
print('  SALT_B64   = "%s"' % base64.b64encode(salt).decode())
print('  HASH_B64   = "%s"' % base64.b64encode(dk).decode())

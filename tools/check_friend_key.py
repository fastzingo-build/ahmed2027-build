#!/usr/bin/env python3
"""Check that a key matches the hash currently in FriendKeyConfig.java.

Usage:  python3 tools/check_friend_key.py          (type/paste the key; nothing is echoed or saved)
This is how the owner (or the friend) can confirm the shipped hash belongs to the key they hold.
"""
import base64, getpass, hashlib, hmac, re, sys, pathlib

cfg = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/java/com/ahmed2027/app/friendkey/FriendKeyConfig.java"
src = cfg.read_text()
def grab(name):
    return re.search(name + r'\s*=\s*"?([^";]+)"?;', src).group(1)
iters = int(grab("ITERATIONS")); salt = base64.b64decode(grab("SALT_B64")); want = base64.b64decode(grab("HASH_B64"))
key = getpass.getpass("Friend Key: ").strip()
got = hashlib.pbkdf2_hmac("sha256", key.encode("utf-8"), salt, iters, dklen=len(want))
ok = hmac.compare_digest(got, want)
print("MATCH: this key opens AHMED." if ok else "NO MATCH: this key does not match the shipped hash.")
sys.exit(0 if ok else 1)

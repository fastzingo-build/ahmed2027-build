package com.ahmed2027.app.core;

import static org.junit.Assert.*;

import com.ahmed2027.app.friendkey.*;
import com.ahmed2027.app.launcher.AppClass;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class FriendKeyTest {
    private static final long MIN = 60_000L;

    private static byte[] hex(String h) {
        byte[] b = new byte[h.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(h.substring(2 * i, 2 * i + 2), 16);
        return b;
    }

    // ---- KeyVerifier
    @Test public void rfc7914KnownAnswerVector() {
        // PBKDF2-HMAC-SHA-256, P="passwd", S="salt", c=1 (RFC 7914 section 11), first 32 bytes
        byte[] expected = hex("55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc");
        assertTrue(KeyVerifier.verify("passwd".toCharArray(), "salt".getBytes(), 1, expected));
    }

    private static final class Fixture {
        final byte[] salt = new byte[16]; final char[] key = new char[48]; byte[] hash;
        Fixture() throws Exception {
            SecureRandom r = new SecureRandom(); r.nextBytes(salt);
            String abc = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
            for (int i = 0; i < key.length; i++) key[i] = abc.charAt(r.nextInt(abc.length()));
            hash = KeyVerifier.derive(key.clone(), salt, 1000, 32);
        }
    }

    @Test public void correctKeyOpens() throws Exception {
        Fixture f = new Fixture();
        assertTrue(KeyVerifier.verify(f.key.clone(), f.salt, 1000, f.hash));
    }

    @Test public void oneWrongCharacterFailsAtAnyPosition() throws Exception {
        Fixture f = new Fixture();
        for (int pos : new int[]{0, 17, 47}) {
            char[] bad = f.key.clone();
            bad[pos] = bad[pos] == 'x' ? 'y' : 'x';
            assertFalse("pos " + pos, KeyVerifier.verify(bad, f.salt, 1000, f.hash));
        }
        assertFalse(KeyVerifier.verify(new String(f.key).substring(1).toCharArray(), f.salt, 1000, f.hash));
        assertFalse(KeyVerifier.verify((new String(f.key) + "x").toCharArray(), f.salt, 1000, f.hash));
        assertFalse(KeyVerifier.verify(new char[0], f.salt, 1000, f.hash));
        assertFalse(KeyVerifier.verify(null, f.salt, 1000, f.hash));
    }

    @Test public void inputArrayIsZeroedAfterVerify() throws Exception {
        Fixture f = new Fixture();
        char[] in = f.key.clone();
        KeyVerifier.verify(in, f.salt, 1000, f.hash);
        for (char c : in) assertEquals('\0', c);
        char[] bad = "wrong".toCharArray();
        KeyVerifier.verify(bad, f.salt, 1000, f.hash);
        for (char c : bad) assertEquals('\0', c);
    }

    @Test public void shippedConfigIsWellFormed() {
        assertEquals("PBKDF2WithHmacSHA256", FriendKeyConfig.ALGO);
        assertEquals(310000, FriendKeyConfig.ITERATIONS);
        assertEquals(16, Base64.getDecoder().decode(FriendKeyConfig.SALT_B64).length);
        assertEquals(32, Base64.getDecoder().decode(FriendKeyConfig.HASH_B64).length);
    }

    @Test public void shippedConfigRejectsGarbageWithRealParameters() {
        assertFalse(FriendKey.verify("definitely-not-the-friend-key".toCharArray()));
    }

    // ---- AttemptLimiter
    static final class MemLimiterStore implements AttemptLimiter.Store {
        AttemptLimiter.State s = new AttemptLimiter.State();
        public AttemptLimiter.State load() { AttemptLimiter.State c = new AttemptLimiter.State();
            c.failures = s.failures; c.lockUntilWall = s.lockUntilWall; c.lockDelayMs = s.lockDelayMs;
            c.lockStartElapsed = s.lockStartElapsed; c.highWater = s.highWater; return c; }
        public void save(AttemptLimiter.State n) { s = n; }
    }

    @Test public void escalatingDelays() {
        assertEquals(0, AttemptLimiter.delayForFailures(1));
        assertEquals(0, AttemptLimiter.delayForFailures(2));
        assertEquals(1 * MIN, AttemptLimiter.delayForFailures(3));
        assertEquals(5 * MIN, AttemptLimiter.delayForFailures(4));
        assertEquals(15 * MIN, AttemptLimiter.delayForFailures(5));
        assertEquals(60 * MIN, AttemptLimiter.delayForFailures(6));
        assertEquals(60 * MIN, AttemptLimiter.delayForFailures(40));
    }

    @Test public void thirdWrongLocksForOneMinuteThenAllowsAgain() {
        MemLimiterStore st = new MemLimiterStore(); AttemptLimiter l = new AttemptLimiter(st);
        long w = 1_000_000_000L, e = 5000;
        l.recordFailure(w, e); l.recordFailure(w, e);
        assertTrue(l.canAttempt(w, e));
        assertEquals(MIN, l.recordFailure(w, e));
        assertFalse(l.canAttempt(w + 30_000, e + 30_000));
        assertEquals(30_000, l.remainingMillis(w + 30_000, e + 30_000));
        assertTrue(l.canAttempt(w + MIN, e + MIN));
        assertEquals(5 * MIN, l.recordFailure(w + MIN, e + MIN)); // 4th
    }

    @Test public void delayPersistsAcrossProcessKill() {
        MemLimiterStore st = new MemLimiterStore();
        long w = 1_000_000_000L, e = 5000;
        AttemptLimiter a = new AttemptLimiter(st);
        a.recordFailure(w, e); a.recordFailure(w, e); a.recordFailure(w, e);
        AttemptLimiter afterKill = new AttemptLimiter(st); // new process, same persisted store
        assertEquals(3, afterKill.failures());
        assertFalse(afterKill.canAttempt(w + 10_000, e + 10_000));
    }

    @Test public void delayPersistsAcrossReboot() {
        MemLimiterStore st = new MemLimiterStore(); AttemptLimiter l = new AttemptLimiter(st);
        long w = 1_000_000_000L;
        for (int i = 0; i < 4; i++) l.recordFailure(w, 900_000);   // 5 min lock
        // reboot after 1 min: elapsedRealtime restarted, wall clock continues
        assertEquals(4 * MIN, l.remainingMillis(w + MIN, 20_000));
    }

    @Test public void clockSetForwardDoesNotSkipTheDelay() {
        MemLimiterStore st = new MemLimiterStore(); AttemptLimiter l = new AttemptLimiter(st);
        long w = 1_000_000_000L, e = 5000;
        for (int i = 0; i < 3; i++) l.recordFailure(w, e);
        assertFalse(l.canAttempt(w + 10 * MIN, e + 10_000)); // wall says done, elapsed says 50 s left
    }

    @Test public void clockSetBackNeverLengthensBeyondTheOriginalDelay() {
        MemLimiterStore st = new MemLimiterStore(); AttemptLimiter l = new AttemptLimiter(st);
        long w = 1_000_000_000L, e = 5000;
        for (int i = 0; i < 3; i++) l.recordFailure(w, e);
        long rem = l.remainingMillis(w - 3_600_000, e + 30_000);
        assertTrue("still locked", rem >= 30_000);
        assertTrue("never more than the delay (" + rem + ")", rem <= MIN);
    }

    @Test public void successResetsEverything() {
        MemLimiterStore st = new MemLimiterStore(); AttemptLimiter l = new AttemptLimiter(st);
        for (int i = 0; i < 5; i++) l.recordFailure(1_000_000_000L, 5000);
        l.recordSuccess();
        assertEquals(0, l.failures());
        assertTrue(l.canAttempt(1_000_000_001L, 5001));
        assertEquals(0, l.recordFailure(1_000_000_001L, 5001));
    }

    // ---- UnlockManager
    static final class MemUnlockStore implements UnlockManager.Store {
        final Map<String, String> m = new HashMap<>();
        public String get(String k) { return m.get(k); }
        public void put(String k, String v) { m.put(k, v); }
        public void remove(String k) { m.remove(k); }
    }

    @Test public void appUnlockLasts15MinutesAndOnlyForThatApp() {
        UnlockManager u = new UnlockManager(new MemUnlockStore());
        long w = 2_000_000_000L, e = 10_000;
        u.grant(UnlockManager.APP, "com.insta", w, e);
        assertTrue(u.isActive(UnlockManager.APP, "com.insta", w + 14 * MIN, e + 14 * MIN));
        assertFalse(u.isActive(UnlockManager.APP, "com.insta", w + 15 * MIN, e + 15 * MIN));
        assertFalse(u.isActive(UnlockManager.APP, "com.other", w, e));
        assertFalse(u.isActive(UnlockManager.SETTINGS, null, w, e));
    }

    @Test public void settingsUnlockLasts10Minutes() {
        UnlockManager u = new UnlockManager(new MemUnlockStore());
        long w = 2_000_000_000L, e = 10_000;
        u.grant(UnlockManager.SETTINGS, null, w, e);
        assertTrue(u.isActive(UnlockManager.SETTINGS, null, w + 9 * MIN, e + 9 * MIN));
        assertFalse(u.isActive(UnlockManager.SETTINGS, null, w + 10 * MIN, e + 10 * MIN));
    }

    @Test public void clockMovedBackCannotExtendAnUnlock() {
        UnlockManager u = new UnlockManager(new MemUnlockStore());
        long w = 2_000_000_000L, e = 10_000;
        u.grant(UnlockManager.APP, "a", w, e);
        assertFalse(u.isActive(UnlockManager.APP, "a", w - 3_600_000, e + 20 * MIN)); // elapsed says over
        assertFalse(u.isActive(UnlockManager.APP, "a", w - 3_600_000, 5)); // after reboot: wall far behind -> invalid
    }

    @Test public void clockMovedForwardEndsAnUnlockEarly() {
        UnlockManager u = new UnlockManager(new MemUnlockStore());
        long w = 2_000_000_000L, e = 10_000;
        u.grant(UnlockManager.APP, "a", w, e);
        assertFalse(u.isActive(UnlockManager.APP, "a", w + 20 * MIN, e + MIN));
    }

    @Test public void revokeAndCorruptValuesAreLocked() {
        MemUnlockStore s = new MemUnlockStore(); UnlockManager u = new UnlockManager(s);
        u.grant(UnlockManager.SETTINGS, null, 1, 1);
        u.revoke(UnlockManager.SETTINGS, null);
        assertFalse(u.isActive(UnlockManager.SETTINGS, null, 2, 2));
        s.put("APP|x", "garbage");
        assertFalse(u.isActive(UnlockManager.APP, "x", 2, 2));
    }

    // ---- CoolingOff
    @Test public void coolingOffWaitsFiveMinutes() {
        long w = 1_000_000_000L, e = 100_000;
        assertEquals(CoolingOff.DURATION_MS, CoolingOff.remainingMillis(w, e, w, e));
        assertEquals(1 * MIN, CoolingOff.remainingMillis(w, e, w + 4 * MIN, e + 4 * MIN));
        assertFalse(CoolingOff.ready(true, w, e, w + 4 * MIN, e + 4 * MIN));
        assertTrue(CoolingOff.ready(true, w, e, w + 5 * MIN, e + 5 * MIN));
        assertFalse(CoolingOff.ready(false, 0, 0, w, e));
    }

    @Test public void coolingOffCannotBeSkippedByClockForwardAndGoesStale() {
        long w = 1_000_000_000L, e = 100_000;
        assertFalse(CoolingOff.ready(true, w, e, w + 60 * MIN, e + MIN));
        assertFalse(CoolingOff.ready(true, w, e, w + 40 * MIN, e + 40 * MIN)); // stale after 30 min
    }

    // ---- Protection
    @Test public void onlyRelaxingChangesNeedTheKey() {
        assertTrue(Protection.classChangeNeedsKey(AppClass.BLOCKED, AppClass.ALLOWED));
        assertTrue(Protection.classChangeNeedsKey(AppClass.BLOCKED, AppClass.LIMITED));
        assertTrue(Protection.classChangeNeedsKey(AppClass.LIMITED, AppClass.ALLOWED));
        assertFalse(Protection.classChangeNeedsKey(AppClass.ALLOWED, AppClass.BLOCKED));
        assertFalse(Protection.classChangeNeedsKey(AppClass.ALLOWED, AppClass.LIMITED));
        assertFalse(Protection.classChangeNeedsKey(AppClass.HIDDEN, AppClass.ALLOWED));
        assertFalse(Protection.classChangeNeedsKey(AppClass.BLOCKED, AppClass.BLOCKED));
    }
}

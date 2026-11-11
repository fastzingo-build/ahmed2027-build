package com.ahmed2027.app.core;

import static org.junit.Assert.*;

import com.ahmed2027.app.social.SocialSessionManager;
import com.ahmed2027.app.social.SocialSessionManager.Session;
import com.ahmed2027.app.social.SocialSessionManager.State;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocialSessionManagerTest {
    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");
    private static final ZoneId NY = ZoneId.of("America/New_York");
    private static final long MIN = 60_000L;
    private static final String TT = SocialSessionManager.KEY_TIKTOK, IG = SocialSessionManager.KEY_INSTAGRAM;

    /** In-memory store: survives "process restarts" because tests reuse the same instance. */
    static final class MemStore implements SocialSessionManager.Store {
        final Map<String, Session> rows = new HashMap<>();
        long hw; final List<String> logs = new ArrayList<>();
        public Session find(String k, String d) { return rows.get(k + "|" + d); }
        public boolean insert(Session s) { return rows.putIfAbsent(s.key + "|" + s.localDate, s) == null; }
        public long highWater() { return hw; }
        public void setHighWater(long w) { hw = w; }
        public void log(String e) { logs.add(e); }
    }

    private static long ms(LocalDateTime t, ZoneId z) { return t.atZone(z).toInstant().toEpochMilli(); }
    private static long tunis(int d, int h, int m) { return ms(LocalDateTime.of(2026, 11, d, h, m), TUNIS); }

    @Test public void startsEligibleThenActiveThenExpired() {
        MemStore st = new MemStore(); SocialSessionManager m = new SocialSessionManager(st, TUNIS);
        long t = tunis(10, 12, 0);
        assertEquals(State.ELIGIBLE, m.state(TT, t, 1000));
        assertNotNull(m.start(TT, t, 1000));
        assertEquals(30 * MIN, m.remainingMillis(TT, t, 1000));
        assertEquals(State.ACTIVE, m.state(TT, t + 29 * MIN, 1000 + 29 * MIN));
        assertEquals(1 * MIN, m.remainingMillis(TT, t + 29 * MIN, 1000 + 29 * MIN));
        assertEquals(State.EXPIRED, m.state(TT, t + 30 * MIN, 1000 + 30 * MIN));
    }

    @Test public void secondStartSameDayIsRefused() {
        MemStore st = new MemStore(); SocialSessionManager m = new SocialSessionManager(st, TUNIS);
        long t = tunis(10, 9, 0);
        assertNotNull(m.start(TT, t, 0));
        assertNull(m.start(TT, t + 2 * 60 * MIN, 2 * 60 * MIN));
    }

    @Test public void leavingAndReturningDoesNotReset() {
        MemStore st = new MemStore(); SocialSessionManager m = new SocialSessionManager(st, TUNIS);
        long t = tunis(10, 9, 0);
        m.start(TT, t, 0);
        // user leaves for 10 min, returns: the clock kept running
        assertEquals(20 * MIN, m.remainingMillis(TT, t + 10 * MIN, 10 * MIN));
        assertNull(m.start(TT, t + 10 * MIN, 10 * MIN));
    }

    @Test public void midnightBoundaryStart2350() {
        MemStore st = new MemStore(); SocialSessionManager m = new SocialSessionManager(st, TUNIS);
        long t = tunis(10, 23, 50);
        Session s = m.start(IG, t, 5000);
        assertEquals(tunis(11, 0, 0), s.expiryWall);
        assertEquals(10 * MIN, m.remainingMillis(IG, t, 5000));
        assertEquals(State.ACTIVE, m.state(IG, tunis(10, 23, 59), 5000 + 9 * MIN));
        // at 00:00 a NEW local date begins: eligible again
        assertEquals(State.ELIGIBLE, m.state(IG, tunis(11, 0, 0), 5000 + 10 * MIN));
        assertNotNull(m.start(IG, tunis(11, 0, 1), 5000 + 10 * MIN + 1000));
    }

    @Test public void independentQuotas() {
        MemStore st = new MemStore(); SocialSessionManager m = new SocialSessionManager(st, TUNIS);
        long t = tunis(10, 12, 0);
        m.start(TT, t, 0);
        assertEquals(State.ELIGIBLE, m.state(IG, t, 0));
        assertNotNull(m.start(IG, t, 0));
        assertEquals(State.ACTIVE, m.state(TT, t, 0));
    }

    @Test public void tiktokPackagesShareOneQuota() {
        assertEquals(TT, SocialSessionManager.quotaKey("com.zhiliaoapp.musically"));
        assertEquals(TT, SocialSessionManager.quotaKey("com.ss.android.ugc.trill"));
        assertEquals(IG, SocialSessionManager.quotaKey("com.instagram.android"));
        assertNull(SocialSessionManager.quotaKey("com.whatsapp"));
    }

    @Test public void restartKeepsSessionBecauseStateIsPersisted() {
        MemStore st = new MemStore();
        long t = tunis(10, 12, 0);
        new SocialSessionManager(st, TUNIS).start(TT, t, 100);
        SocialSessionManager afterRestart = new SocialSessionManager(st, TUNIS); // new process
        assertEquals(State.ACTIVE, afterRestart.state(TT, t + 5 * MIN, 100 + 5 * MIN));
        assertNull(afterRestart.start(TT, t + 5 * MIN, 100 + 5 * MIN));
    }

    @Test public void rebootDoesNotGrantAnotherSession() {
        MemStore st = new MemStore();
        long t = tunis(10, 12, 0);
        new SocialSessionManager(st, TUNIS).start(TT, t, 50 * MIN); // device had been up 50 min
        // reboot after 5 min; elapsedRealtime restarts near 0, wall clock continues
        SocialSessionManager m = new SocialSessionManager(st, TUNIS);
        long wall = t + 8 * MIN;
        assertEquals(State.ACTIVE, m.state(TT, wall, 3 * MIN));
        assertEquals(22 * MIN, m.remainingMillis(TT, wall, 3 * MIN));
        assertNull(m.start(TT, wall, 3 * MIN));
        // and it still expires on wall time
        assertEquals(State.EXPIRED, m.state(TT, t + 31 * MIN, 26 * MIN));
    }

    @Test public void timezoneChangeDoesNotCreateANewDay() {
        MemStore st = new MemStore();
        long t = tunis(10, 22, 0);                          // 22:00 Tunis = 07:00 next day in Tokyo
        new SocialSessionManager(st, TUNIS).start(TT, t, 0);
        // device timezone changes; manager still uses the persisted HOME zone
        SocialSessionManager m = new SocialSessionManager(st, TUNIS);
        assertEquals(State.EXPIRED, m.state(TT, t + 40 * MIN, 40 * MIN));
        assertNull(m.start(TT, t + 40 * MIN, 40 * MIN));
        // a manager wrongly using Tokyo would think it is a new day: this is why the zone is fixed
        SocialSessionManager wrong = new SocialSessionManager(new MemStore(), ZoneId.of("Asia/Tokyo"));
        assertEquals(State.ELIGIBLE, wrong.state(TT, t, 0));
    }

    @Test public void dstDayStillExpiresAtLocalMidnight() {
        MemStore st = new MemStore(); SocialSessionManager m = new SocialSessionManager(st, NY);
        long t = ms(LocalDateTime.of(2026, 3, 8, 23, 50), NY);  // spring-forward day
        Session s = m.start(TT, t, 0);
        assertEquals(ms(LocalDateTime.of(2026, 3, 9, 0, 0), NY), s.expiryWall);
        assertEquals(10 * MIN, s.expiryWall - s.startWall);
    }

    @Test public void clockMovedBackDuringSessionDoesNotExtendIt() {
        MemStore st = new MemStore(); SocialSessionManager m = new SocialSessionManager(st, TUNIS);
        long t = tunis(10, 12, 0);
        m.start(TT, t, 0);
        // 20 real minutes pass, but the user sets the wall clock back 1 hour
        long wall = t + 20 * MIN - 60 * MIN;
        assertEquals(10 * MIN, m.remainingMillis(TT, wall, 20 * MIN));
        assertTrue(m.clockSuspect(wall));
    }

    @Test public void clockMovedBackAfterExpiryDoesNotGrantNewSession() {
        MemStore st = new MemStore(); SocialSessionManager m = new SocialSessionManager(st, TUNIS);
        long t = tunis(10, 12, 0);
        m.start(TT, t, 0);
        assertEquals(State.EXPIRED, m.state(TT, t + 31 * MIN, 31 * MIN));
        long back = t - 60 * MIN;
        assertEquals(State.EXPIRED, m.state(TT, back, 40 * MIN));
        assertNull(m.start(TT, back, 40 * MIN));
        assertFalse(st.logs.isEmpty());
    }

    @Test public void clockMovedBackToAnEarlierUnusedDateGrantsNothing() {
        MemStore st = new MemStore(); SocialSessionManager m = new SocialSessionManager(st, TUNIS);
        long t = tunis(10, 12, 0);
        assertNotNull(m.start(TT, t, 0));
        assertEquals(State.EXPIRED, m.state(TT, t + 31 * MIN, 31 * MIN)); // high-water moves to 12:31
        long back = tunis(8, 12, 0);                                       // two days earlier, never used
        assertEquals(State.EXPIRED, m.state(TT, back, 60 * MIN));
        assertNull(m.start(TT, back, 60 * MIN));
    }

    @Test public void clockMovedForwardEndsSessionNotExtendsIt() {
        MemStore st = new MemStore(); SocialSessionManager m = new SocialSessionManager(st, TUNIS);
        long t = tunis(10, 12, 0);
        m.start(TT, t, 0);
        assertEquals(State.EXPIRED, m.state(TT, t + 60 * MIN, 5 * MIN));
    }
}

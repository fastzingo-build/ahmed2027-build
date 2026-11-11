package com.ahmed2027.app.social;

import com.ahmed2027.app.core.ClockMath;

import java.time.ZoneId;

/**
 * Daily social-media session rules. Pure Java (no Android), tested with JUnit.
 *
 * Rules: one session per quota key (tiktok / instagram) per local date, started only on an
 * explicit start() call. expiry = min(start + 30 min, next local midnight). Leaving/returning
 * never resets anything because state lives in the Store, not in memory.
 *
 * "Local date" is computed in a fixed HOME zone (persisted by the Android store), so changing
 * the phone's timezone does not create a new day.
 *
 * Clock tampering: a persisted high-water mark of wall time is kept. The effective "now" is
 * max(wall now, high-water). Setting the clock back therefore cannot reach an earlier, unused
 * date nor extend a running session. Remaining time of an ACTIVE session is
 * min(wall-based, elapsedRealtime-based). Setting the clock FORWARD cannot be told apart from
 * time passing; it can only reach later dates, each of which still has just one session.
 * This cannot be made tamper-proof; see docs/LIMITATIONS.md.
 */
public final class SocialSessionManager {
    public enum State { ELIGIBLE, ACTIVE, EXPIRED }

    public static final long SESSION_MS = 30L * 60_000L;
    public static final long CLOCK_TOLERANCE_MS = 2L * 60_000L;

    public static final String KEY_TIKTOK = "tiktok";
    public static final String KEY_INSTAGRAM = "instagram";

    public static final class Session {
        public final String key, localDate;
        public final long startWall, startElapsed, expiryWall;
        public Session(String key, String localDate, long startWall, long startElapsed, long expiryWall) {
            this.key = key; this.localDate = localDate;
            this.startWall = startWall; this.startElapsed = startElapsed; this.expiryWall = expiryWall;
        }
    }

    /** Persistence. Implementations must make insert() atomic (UNIQUE key + localDate). */
    public interface Store {
        Session find(String key, String localDate);
        /** @return false if a session for (key, localDate) already exists. */
        boolean insert(Session s);
        long highWater();               // 0 when none
        void setHighWater(long wall);
        void log(String event);
    }

    private final Store store;
    private final ZoneId homeZone;

    public SocialSessionManager(Store store, ZoneId homeZone) {
        this.store = store;
        this.homeZone = homeZone;
    }

    /** Maps an Android package to its quota key, or null when it is not a social app. */
    public static String quotaKey(String pkg) {
        if ("com.zhiliaoapp.musically".equals(pkg) || "com.ss.android.ugc.trill".equals(pkg)) return KEY_TIKTOK;
        if ("com.instagram.android".equals(pkg)) return KEY_INSTAGRAM;
        return null;
    }

    private long effective(long nowWall) {
        long hw = store.highWater();
        long eff = Math.max(nowWall, hw);
        if (eff > hw) store.setHighWater(eff);
        return eff;
    }

    /** True when the wall clock is behind what we have already seen (clock moved back). */
    public boolean clockSuspect(long nowWall) {
        return store.highWater() - nowWall > CLOCK_TOLERANCE_MS;
    }

    private String date(long wall) { return ClockMath.localDate(wall, homeZone).toString(); }

    public State state(String key, long nowWall, long nowElapsed) {
        long eff = effective(nowWall);
        Session s = store.find(key, date(eff));
        if (s == null) return State.ELIGIBLE;
        return remainingOf(s, eff, nowElapsed) > 0 ? State.ACTIVE : State.EXPIRED;
    }

    /** Milliseconds left in today's session; 0 when ELIGIBLE or EXPIRED. */
    public long remainingMillis(String key, long nowWall, long nowElapsed) {
        long eff = effective(nowWall);
        Session s = store.find(key, date(eff));
        return s == null ? 0 : remainingOf(s, eff, nowElapsed);
    }

    private static long remainingOf(Session s, long effWall, long nowElapsed) {
        long byWall = s.expiryWall - effWall;
        long allowed = s.expiryWall - s.startWall;
        // Elapsed reference is invalid after a reboot (elapsedRealtime restarts): wall only then.
        long byElapsed = nowElapsed >= s.startElapsed ? allowed - (nowElapsed - s.startElapsed) : Long.MAX_VALUE;
        return Math.max(0, Math.min(byWall, byElapsed));
    }

    /**
     * Explicit start. Returns the new session, or null when today's session was already started
     * (active or used). Must only be called after the user's explicit tap.
     */
    public Session start(String key, long nowWall, long nowElapsed) {
        if (clockSuspect(nowWall)) store.log("clock_back key=" + key + " by_ms=" + (store.highWater() - nowWall));
        long eff = effective(nowWall);
        long expiry = Math.min(eff + SESSION_MS, ClockMath.nextLocalMidnight(eff, homeZone));
        Session s = new Session(key, date(eff), eff, nowElapsed, expiry);
        return store.insert(s) ? s : null;
    }
}

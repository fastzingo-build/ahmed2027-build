package com.ahmed2027.app.friendkey;

/** Optional 5-minute wait before the key field appears. Pure; the start time is persisted by the caller. */
public final class CoolingOff {
    public static final long DURATION_MS = 5L * 60_000L;
    public static final long READY_WINDOW_MS = 30L * 60_000L;

    private CoolingOff() {}

    /** Longest of the two views = stricter (clock moved forward cannot skip the wait). */
    public static long remainingMillis(long startWall, long startElapsed, long nowWall, long nowElapsed) {
        long byWall = DURATION_MS - (nowWall - startWall);
        long r = byWall;
        if (nowElapsed >= startElapsed) r = Math.max(r, DURATION_MS - (nowElapsed - startElapsed));
        return Math.max(0, r);
    }

    /** Wait finished and still fresh (so the field does not stay open forever). */
    public static boolean ready(boolean started, long startWall, long startElapsed, long nowWall, long nowElapsed) {
        if (!started) return false;
        if (remainingMillis(startWall, startElapsed, nowWall, nowElapsed) > 0) return false;
        long age = nowElapsed >= startElapsed ? nowElapsed - startElapsed : nowWall - startWall;
        return age >= 0 && age <= READY_WINDOW_MS && nowWall - startWall <= READY_WINDOW_MS;
    }
}

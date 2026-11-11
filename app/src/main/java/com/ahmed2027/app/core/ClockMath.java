package com.ahmed2027.app.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Pure time math, no Android deps, so JUnit can test it. Used later by the
 * social-session state machine (expiry = min(start + 30 min, next local midnight)).
 */
public final class ClockMath {
    private ClockMath() {}

    /** Local calendar date for a wall-clock instant in the given zone. */
    public static LocalDate localDate(long wallMillis, ZoneId zone) {
        return Instant.ofEpochMilli(wallMillis).atZone(zone).toLocalDate();
    }

    /** Wall-clock millis of the next local midnight strictly after wallMillis (DST-safe). */
    public static long nextLocalMidnight(long wallMillis, ZoneId zone) {
        LocalDate d = localDate(wallMillis, zone);
        return d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /**
     * Drift between wall-clock progress and monotonic progress since a reference point.
     * 0 means they agree. Positive: wall clock moved forward more than real time
     * (or time was set ahead). Negative: wall clock moved backwards / lagged.
     */
    public static long driftMillis(long refWall, long refElapsed, long nowWall, long nowElapsed) {
        long wallDelta = nowWall - refWall;
        long elapsedDelta = nowElapsed - refElapsed;
        return wallDelta - elapsedDelta;
    }

    /**
     * True when drift exceeds the tolerance. Note: elapsedRealtime keeps counting in
     * deep sleep but resets on reboot; callers must treat a reboot (elapsed went
     * backwards) as "reference unusable", not as tampering.
     */
    public static boolean isJump(long refWall, long refElapsed, long nowWall, long nowElapsed,
                                 long toleranceMillis) {
        if (nowElapsed < refElapsed) return false; // reboot: reference is invalid
        return Math.abs(driftMillis(refWall, refElapsed, nowWall, nowElapsed)) > toleranceMillis;
    }
}

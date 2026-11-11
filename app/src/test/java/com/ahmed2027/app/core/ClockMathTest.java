package com.ahmed2027.app.core;

import static org.junit.Assert.*;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class ClockMathTest {
    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");
    private static final ZoneId NY = ZoneId.of("America/New_York");

    private static long ms(LocalDateTime t, ZoneId z) { return t.atZone(z).toInstant().toEpochMilli(); }

    @Test public void nextMidnightIsStrictlyAfter() {
        long t = ms(LocalDateTime.of(2026, 11, 10, 23, 50), TUNIS);
        assertEquals(ms(LocalDateTime.of(2026, 11, 11, 0, 0), TUNIS), ClockMath.nextLocalMidnight(t, TUNIS));
        long mid = ms(LocalDateTime.of(2026, 11, 11, 0, 0), TUNIS);
        assertEquals(ms(LocalDateTime.of(2026, 11, 12, 0, 0), TUNIS), ClockMath.nextLocalMidnight(mid, TUNIS));
    }

    @Test public void midnightIs10MinutesAfter2350() {
        long t = ms(LocalDateTime.of(2026, 11, 10, 23, 50), TUNIS);
        assertEquals(10 * 60_000L, ClockMath.nextLocalMidnight(t, TUNIS) - t);
    }

    @Test public void dstDayIs23HoursLong() {
        // US spring-forward 2026-03-08: local day lasts 23h
        long t = ms(LocalDateTime.of(2026, 3, 8, 0, 30), NY);
        long next = ClockMath.nextLocalMidnight(t, NY);
        assertEquals(ms(LocalDateTime.of(2026, 3, 9, 0, 0), NY), next);
        assertEquals(23 * 3_600_000L - 30 * 60_000L, next - t);
    }

    @Test public void localDateDependsOnZone() {
        long t = ms(LocalDateTime.of(2026, 11, 10, 23, 30), TUNIS); // 22:30 UTC
        assertEquals(10, ClockMath.localDate(t, TUNIS).getDayOfMonth());
        assertEquals(11, ClockMath.localDate(t, ZoneId.of("Asia/Tokyo")).getDayOfMonth());
    }

    @Test public void noDriftWhenClocksAgree() {
        assertEquals(0, ClockMath.driftMillis(1000, 50, 61_000, 60_050));
        assertFalse(ClockMath.isJump(1000, 50, 61_000, 60_050, 5_000));
    }

    @Test public void wallClockMovedBackIsAJump() {
        // 1 minute of real time passed but wall clock went back 1 hour
        assertTrue(ClockMath.isJump(10_000_000, 0, 10_000_000 - 3_600_000, 60_000, 5_000));
        assertTrue(ClockMath.driftMillis(10_000_000, 0, 10_000_000 - 3_600_000, 60_000) < 0);
    }

    @Test public void wallClockMovedForwardIsAJump() {
        assertTrue(ClockMath.isJump(0, 0, 3_600_000 + 60_000, 60_000, 5_000));
    }

    @Test public void rebootIsNotTampering() {
        // elapsedRealtime restarted from ~0: reference invalid, not a jump
        assertFalse(ClockMath.isJump(5_000_000, 9_000_000, 5_100_000, 20_000, 5_000));
    }
}


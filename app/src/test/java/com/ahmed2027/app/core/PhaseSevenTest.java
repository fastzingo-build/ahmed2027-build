package com.ahmed2027.app.core;

import static org.junit.Assert.*;

import com.ahmed2027.app.analytics.Series;
import com.ahmed2027.app.creator.Pipeline;
import com.ahmed2027.app.focus.QuietPeriods;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PhaseSevenTest {
    @Test public void quietOvernight() {
        QuietPeriods.Period p = QuietPeriods.parse("22:00-06:00");
        assertTrue(p.contains(23 * 60)); assertTrue(p.contains(5 * 60 + 59)); assertFalse(p.contains(6 * 60)); assertFalse(p.contains(12 * 60));
    }
    @Test public void quietSameDay() {
        QuietPeriods.Period p = QuietPeriods.parse("13:00-14:30");
        assertTrue(p.contains(13 * 60)); assertFalse(p.contains(14 * 60 + 30)); assertFalse(p.contains(12 * 60));
    }
    @Test public void quietParseRejectsJunk() {
        assertNull(QuietPeriods.parse("25:00-06:00")); assertNull(QuietPeriods.parse("abc")); assertNull(QuietPeriods.parse("10:00"));
        assertEquals("22:05-06:00", QuietPeriods.parse("22:05-06:00").format());
    }
    @Test public void emptyPeriodNeverActive() { assertFalse(new QuietPeriods.Period(600, 600).contains(600)); }
    @Test public void nextBoundary() {
        java.util.List<QuietPeriods.Period> ps = Arrays.asList(QuietPeriods.parse("22:00-06:00"));
        assertEquals(60, QuietPeriods.minutesToNextBoundary(ps, 21 * 60));
        assertEquals(60, QuietPeriods.minutesToNextBoundary(ps, 5 * 60));
        assertEquals(480, QuietPeriods.minutesToNextBoundary(ps, 22 * 60));
        assertEquals(-1, QuietPeriods.minutesToNextBoundary(Arrays.<QuietPeriods.Period>asList(), 100));
    }
    @Test public void pipelineOrder() {
        assertEquals("planning", Pipeline.next("idea")); assertEquals("published", Pipeline.next("ready"));
        assertEquals("published", Pipeline.next("published")); assertTrue(Pipeline.isLast("published")); assertEquals(0, Pipeline.index("zzz"));
    }
    @Test public void weekCount() {
        LocalDate mon = LocalDate.of(2026, 11, 9);
        assertEquals(mon, Pipeline.weekStart(LocalDate.of(2026, 11, 11)));
        assertEquals(mon, Pipeline.weekStart(LocalDate.of(2026, 11, 15)));
        assertEquals(2, Pipeline.inWeek(Arrays.asList("2026-11-09", "2026-11-15", "2026-11-16", "bad"), mon));
    }
    @Test public void seriesFill() {
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("2026-11-11", 30); m.put("2026-11-09", 10); m.put("2026-10-01", 99);
        int[] v = Series.fill(m, LocalDate.of(2026, 11, 11), 7);
        assertArrayEquals(new int[]{0, 0, 0, 0, 10, 0, 30}, v);
        assertEquals(40, Series.total(v)); assertEquals(30, Series.max(v)); assertEquals(2, Series.activeDays(v));
    }
    @Test public void onTime() { assertNull(Series.onTimePct(0, 0)); assertEquals(67, (int) Series.onTimePct(2, 3)); }
}

package com.ahmed2027.app.analytics;

import java.time.LocalDate;
import java.util.Map;

/** Daily series for charts. Pure. */
public final class Series {
    private Series() {}

    /** values[i] for the day (end - (days-1-i)); missing days are 0. */
    public static int[] fill(Map<String, Integer> perDay, LocalDate end, int days) {
        int[] v = new int[days];
        for (int i = 0; i < days; i++) {
            Integer x = perDay.get(end.minusDays(days - 1 - i).toString());
            v[i] = x == null ? 0 : x;
        }
        return v;
    }

    public static int total(int[] v) { int t = 0; for (int x : v) t += x; return t; }
    public static int max(int[] v) { int m = 0; for (int x : v) m = Math.max(m, x); return m; }
    public static int activeDays(int[] v) { int n = 0; for (int x : v) if (x > 0) n++; return n; }

    /** Percentage 0..100 of on-time prayers among logged ones; null when none were logged. */
    public static Integer onTimePct(int onTime, int logged) { return logged <= 0 ? null : Math.round(100f * onTime / logged); }
}

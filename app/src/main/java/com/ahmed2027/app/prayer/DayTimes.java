package com.ahmed2027.app.prayer;

import java.time.LocalDate;
import java.time.LocalTime;

/** Prayer timings of one local date, as minutes since local midnight. Pure data. */
public final class DayTimes {
    public enum Source { API, ESTIMATED }

    public final LocalDate date;
    public final int[] minutes = new int[Prayer.values().length]; // indexed by Prayer.ordinal()
    public final Source source;

    public DayTimes(LocalDate date, int[] minutes, Source source) {
        this.date = date;
        System.arraycopy(minutes, 0, this.minutes, 0, this.minutes.length);
        this.source = source;
    }

    public int minutesOf(Prayer p) { return minutes[p.ordinal()]; }

    public LocalTime timeOf(Prayer p) { return LocalTime.of(minutesOf(p) / 60 % 24, minutesOf(p) % 60); }

    /** Copy with per-prayer manual offsets (minutes, clamped to +-10) applied. */
    public DayTimes withOffsets(int[] offsets) {
        int[] m = new int[minutes.length];
        for (int i = 0; i < m.length; i++) {
            int o = offsets == null || i >= offsets.length ? 0 : Math.max(-10, Math.min(10, offsets[i]));
            m[i] = minutes[i] + o;
        }
        return new DayTimes(date, m, source);
    }
}

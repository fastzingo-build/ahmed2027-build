package com.ahmed2027.app.focus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Quiet-period schedule maths (minutes since local midnight). Overnight periods (22:00-06:00) are supported. Pure. */
public final class QuietPeriods {
    public static final class Period {
        public final int startMin, endMin;
        public Period(int startMin, int endMin) { this.startMin = startMin; this.endMin = endMin; }
        public boolean contains(int m) {
            if (startMin == endMin) return false;
            return startMin < endMin ? (m >= startMin && m < endMin) : (m >= startMin || m < endMin);
        }
        public String format() { return hhmm(startMin) + "-" + hhmm(endMin); }
    }

    private QuietPeriods() {}

    public static String hhmm(int m) { return String.format(Locale.US, "%02d:%02d", m / 60, m % 60); }

    /** "22:00-06:00" → Period, or null if malformed. */
    public static Period parse(String s) {
        if (s == null) return null;
        String[] p = s.split("-");
        if (p.length != 2) return null;
        int a = minutes(p[0]), b = minutes(p[1]);
        return a < 0 || b < 0 ? null : new Period(a, b);
    }

    private static int minutes(String t) {
        String[] q = t.trim().split(":");
        if (q.length != 2) return -1;
        try {
            int h = Integer.parseInt(q[0]), m = Integer.parseInt(q[1]);
            return h < 0 || h > 23 || m < 0 || m > 59 ? -1 : h * 60 + m;
        } catch (NumberFormatException e) { return -1; }
    }

    public static boolean inAny(List<Period> ps, int minuteOfDay) {
        for (Period p : ps) if (p.contains(minuteOfDay)) return true;
        return false;
    }

    /** Minutes from now until the next start or end boundary of any period (1..1440), or -1 when there are none. */
    public static int minutesToNextBoundary(List<Period> ps, int nowMin) {
        int best = -1;
        List<Integer> edges = new ArrayList<Integer>();
        for (Period p : ps) { if (p.startMin != p.endMin) { edges.add(p.startMin); edges.add(p.endMin); } }
        for (int e : edges) {
            int d = Math.floorMod(e - nowMin, 1440);
            if (d == 0) d = 1440;
            if (best < 0 || d < best) best = d;
        }
        return best;
    }
}

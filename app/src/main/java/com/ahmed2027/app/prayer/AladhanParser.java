package com.ahmed2027.app.prayer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure parser for the Aladhan "calendar" response (data[] of {timings{...}, date{gregorian{date:"DD-MM-YYYY"}}}).
 * Strict: any missing/odd value rejects the whole month instead of storing guesses.
 */
public final class AladhanParser {
    private static final Pattern TIMINGS = Pattern.compile("\"timings\"\\s*:\\s*\\{([^}]*)\\}");
    private static final Pattern GREG = Pattern.compile(
            "\"gregorian\"\\s*:\\s*\\{\\s*\"date\"\\s*:\\s*\"(\\d{2})-(\\d{2})-(\\d{4})\"");
    private static final String[] KEYS = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};

    private AladhanParser() {}

    /** "05:15" or "05:15 (CET)" -> minutes since midnight, or -1. */
    public static int parseHm(String s) {
        if (s == null || s.length() < 5) return -1;
        try {
            if (s.charAt(2) != ':') return -1;
            int h = Integer.parseInt(s.substring(0, 2)), m = Integer.parseInt(s.substring(3, 5));
            return h < 24 && m < 60 ? h * 60 + m : -1;
        } catch (NumberFormatException e) { return -1; }
    }

    /** @return one DayTimes per day, or null when the response is not trustworthy. */
    public static List<DayTimes> parse(String json) {
        if (json == null) return null;
        List<String> blocks = new ArrayList<>();
        Matcher t = TIMINGS.matcher(json);
        while (t.find()) blocks.add(t.group(1));
        List<LocalDate> dates = new ArrayList<>();
        Matcher g = GREG.matcher(json);
        while (g.find()) {
            try { dates.add(LocalDate.of(Integer.parseInt(g.group(3)), Integer.parseInt(g.group(2)), Integer.parseInt(g.group(1)))); }
            catch (RuntimeException e) { return null; }
        }
        if (blocks.isEmpty() || blocks.size() != dates.size()) return null;
        List<DayTimes> out = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            int[] m = new int[KEYS.length];
            for (int k = 0; k < KEYS.length; k++) {
                Matcher v = Pattern.compile("\"" + KEYS[k] + "\"\\s*:\\s*\"([^\"]*)\"").matcher(blocks.get(i));
                if (!v.find()) return null;
                m[k] = parseHm(v.group(1));
                if (m[k] < 0) return null;
            }
            // sanity: strictly increasing through the day
            for (int k = 1; k < m.length; k++) if (m[k] <= m[k - 1]) return null;
            out.add(new DayTimes(dates.get(i), m, DayTimes.Source.API));
        }
        return out;
    }
}

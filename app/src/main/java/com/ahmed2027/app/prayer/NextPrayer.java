package com.ahmed2027.app.prayer;

import java.time.ZonedDateTime;

/** Pure "what is current / next / is a prayer window open" logic. */
public final class NextPrayer {
    public static final Prayer[] FIVE = {Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA};

    public static final class Result {
        public final Prayer current;       // null: none (e.g. between sunrise and Dhuhr, or before Fajr)
        public final Prayer next;
        public final ZonedDateTime nextAt;
        Result(Prayer c, Prayer n, ZonedDateTime at) { current = c; next = n; nextAt = at; }
    }

    private NextPrayer() {}

    public static ZonedDateTime at(DayTimes d, Prayer p, ZonedDateTime ref) {
        int m = d.minutesOf(p);
        return ref.toLocalDate().equals(d.date)
                ? ref.withHour(0).withMinute(0).withSecond(0).withNano(0).plusMinutes(m)
                : d.date.atStartOfDay(ref.getZone()).plusMinutes(m);
    }

    public static Result compute(DayTimes today, DayTimes tomorrow, ZonedDateTime now) {
        Prayer current = null;
        for (Prayer p : FIVE) {
            if (!at(today, p, now).isAfter(now)) current = p;
        }
        if (current == Prayer.FAJR && !at(today, Prayer.SUNRISE, now).isAfter(now)) current = null;
        for (Prayer p : FIVE) {
            ZonedDateTime t = at(today, p, now);
            if (t.isAfter(now)) return new Result(current, p, t);
        }
        return new Result(current, Prayer.FAJR, at(tomorrow, Prayer.FAJR, now.plusDays(1)));
    }

    /**
     * End of the "prayer window" (adhan time + windowMinutes) if now lies inside one, else null.
     * Used by the optional prayer-focus window.
     */
    public static ZonedDateTime windowEnd(DayTimes today, ZonedDateTime now, int windowMinutes) {
        for (Prayer p : FIVE) {
            ZonedDateTime start = at(today, p, now);
            ZonedDateTime end = start.plusMinutes(windowMinutes);
            if (!now.isBefore(start) && now.isBefore(end)) return end;
        }
        return null;
    }
}

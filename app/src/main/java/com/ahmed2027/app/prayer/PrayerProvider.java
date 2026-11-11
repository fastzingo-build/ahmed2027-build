package com.ahmed2027.app.prayer;

import android.content.Context;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/** Used by Home's NEXT PRAYER card and by the alarms. Reads the cache only: no network, no blocking. */
public final class PrayerProvider {
    public static final class Next {
        public final Prayer prayer;
        public final long timeWall;           // epoch millis
        public final boolean estimated;
        public Next(Prayer prayer, long timeWall, boolean estimated) {
            this.prayer = prayer; this.timeWall = timeWall; this.estimated = estimated;
        }
    }

    private PrayerProvider() {}

    /** Next of the five prayers (never null: an offline estimate is used when there is no cache). */
    public static Next next(Context c) { return next(c, ZonedDateTime.now(PrayerLocation.ZONE)); }

    public static Next next(Context c, ZonedDateTime now) {
        LocalDate d = now.toLocalDate();
        DayTimes today = PrayerRepo.get(c, d);
        DayTimes tomorrow = PrayerRepo.get(c, d.plusDays(1));
        NextPrayer.Result r = NextPrayer.compute(today, tomorrow, now);
        boolean est = (r.nextAt.toLocalDate().equals(d) ? today : tomorrow).source == DayTimes.Source.ESTIMATED;
        return new Next(r.next, r.nextAt.toInstant().toEpochMilli(), est);
    }

    /** End (epoch millis) of an open prayer-focus window, or 0 when none. Respects the user toggle. */
    public static long focusWindowEnd(Context c, ZonedDateTime now) {
        if (!PrayerSettings.focusWindowOn(c)) return 0;
        ZonedDateTime end = NextPrayer.windowEnd(PrayerRepo.get(c, now.toLocalDate()), now, 10);
        return end == null ? 0 : end.toInstant().toEpochMilli();
    }
}

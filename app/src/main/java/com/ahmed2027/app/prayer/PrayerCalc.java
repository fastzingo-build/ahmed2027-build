package com.ahmed2027.app.prayer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Offline fallback: small, documented solar-position algorithm (the widely used PrayTimes.org
 * formulation of Meeus-style low-precision sun coordinates). Fajr and Isha at 18 degrees below the
 * horizon, standard Asr (shadow = object + 1), sunrise/sunset at -0.833 degrees. Output is
 * ESTIMATED: it can differ from the official Tunisian table by a few minutes.
 *
 * Steps: Julian date -> sun declination and equation of time -> solar noon -> hour angle for each
 * target altitude. Timezone offset comes from java.time for that date (never a fixed offset).
 */
public final class PrayerCalc {
    public static final double FAJR_ANGLE = 18.0, ISHA_ANGLE = 18.0, SUN_ANGLE = 0.833;

    /**
     * Empirical margins (minutes, indexed by Prayer.ordinal(): Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha) that the
     * Tunisian table adds on top of the raw astronomical angles. Measured by comparing this algorithm with the
     * Sidi Bouzid November 2026 table on 7 days (see docs/TESTS.md): Fajr -3, Dhuhr +7, Maghrib +5, Isha +3,
     * Asr 0. Derived from ONE third-party table that does not name its source, so treat as an approximation.
     */
    public static final int[] TUNISIA_MARGIN_MIN = {-3, 0, 7, 0, 5, 3};

    private PrayerCalc() {}

    /** Estimate tuned to the Tunisian convention (raw angles + TUNISIA_MARGIN_MIN). */
    public static DayTimes computeTunisia(LocalDate date, double lat, double lng, ZoneId zone) {
        DayTimes raw = compute(date, lat, lng, zone);
        int[] m = new int[raw.minutes.length];
        for (int i = 0; i < m.length; i++) m[i] = raw.minutes[i] + TUNISIA_MARGIN_MIN[i];
        return new DayTimes(date, m, DayTimes.Source.ESTIMATED);
    }

    private static double dsin(double d) { return Math.sin(Math.toRadians(d)); }
    private static double dcos(double d) { return Math.cos(Math.toRadians(d)); }
    private static double dtan(double d) { return Math.tan(Math.toRadians(d)); }
    private static double darcsin(double x) { return Math.toDegrees(Math.asin(x)); }
    private static double darccos(double x) { return Math.toDegrees(Math.acos(x)); }
    private static double darctan2(double y, double x) { return Math.toDegrees(Math.atan2(y, x)); }
    private static double darccot(double x) { return Math.toDegrees(Math.atan(1.0 / x)); }
    private static double fixAngle(double a) { a = a - 360.0 * Math.floor(a / 360.0); return a; }
    private static double fixHour(double a) { a = a - 24.0 * Math.floor(a / 24.0); return a; }

    /** Julian date at 0h UT of the given calendar date. */
    static double julian(int y, int m, int d) {
        if (m <= 2) { y -= 1; m += 12; }
        double a = Math.floor(y / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    /** @return {declination degrees, equation of time hours} */
    private static double[] sunPosition(double jd) {
        double D = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * D);
        double q = fixAngle(280.459 + 0.98564736 * D);
        double L = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g));
        double e = 23.439 - 0.00000036 * D;
        double ra = darctan2(dcos(e) * dsin(L), dcos(L)) / 15.0;
        double eqt = q / 15.0 - fixHour(ra);
        double decl = darcsin(dsin(e) * dsin(L));
        return new double[]{decl, eqt};
    }

    private static double midDay(double jd, double t) {
        double eqt = sunPosition(jd + t)[1];
        return fixHour(12 - eqt);
    }

    private static double angleTime(double jd, double lat, double angle, double t, boolean ccw) {
        double decl = sunPosition(jd + t)[0];
        double noon = midDay(jd, t);
        double term = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat));
        double v = darccos(Math.max(-1, Math.min(1, term))) / 15.0;
        return noon + (ccw ? -v : v);
    }

    private static double asrTime(double jd, double lat, double factor, double t) {
        double decl = sunPosition(jd + t)[0];
        double angle = -darccot(factor + dtan(Math.abs(lat - decl)));
        return angleTime(jd, lat, angle, t, false);
    }

    /** Computes the six timings for one date. Times are rounded to the nearest minute. */
    public static DayTimes compute(LocalDate date, double lat, double lng, ZoneId zone) {
        double tz = zone.getRules().getOffset(ZonedDateTime.of(date, java.time.LocalTime.NOON, zone).toInstant())
                .getTotalSeconds() / 3600.0;
        double jd = julian(date.getYear(), date.getMonthValue(), date.getDayOfMonth()) - lng / (15.0 * 24.0);
        double adj = tz - lng / 15.0; // convert "UTC-solar hours" to local clock
        double[] t = new double[Prayer.values().length];
        double guess;
        guess = 5.0 / 24; t[Prayer.FAJR.ordinal()] = angleTime(jd, lat, FAJR_ANGLE, guess, true);
        guess = 6.0 / 24; t[Prayer.SUNRISE.ordinal()] = angleTime(jd, lat, SUN_ANGLE, guess, true);
        guess = 12.0 / 24; t[Prayer.DHUHR.ordinal()] = midDay(jd, guess);
        guess = 13.0 / 24; t[Prayer.ASR.ordinal()] = asrTime(jd, lat, 1.0, guess);
        guess = 18.0 / 24; t[Prayer.MAGHRIB.ordinal()] = angleTime(jd, lat, SUN_ANGLE, guess, false);
        guess = 18.0 / 24; t[Prayer.ISHA.ordinal()] = angleTime(jd, lat, ISHA_ANGLE, guess, false);
        int[] m = new int[t.length];
        for (int i = 0; i < t.length; i++) {
            double local = fixHour(t[i] + adj);
            m[i] = (int) Math.round(local * 60.0);
        }
        return new DayTimes(date, m, DayTimes.Source.ESTIMATED);
    }
}

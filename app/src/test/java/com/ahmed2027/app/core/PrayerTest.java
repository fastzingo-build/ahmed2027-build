package com.ahmed2027.app.core;

import static org.junit.Assert.*;

import com.ahmed2027.app.prayer.*;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class PrayerTest {
    private static final ZoneId TUN = PrayerLocation.ZONE;

    // Sidi Bouzid city, Nov 2026, third-party table (tunisienumerique.com): day, Fajr, Dhuhr, Asr, Maghrib, Isha
    private static final String[][] TABLE = {
            {"1", "05:15", "12:13", "15:05", "17:33", "18:56"}, {"5", "05:18", "12:13", "15:02", "17:29", "18:53"},
            {"10", "05:22", "12:13", "14:59", "17:25", "18:50"}, {"15", "05:26", "12:14", "14:56", "17:21", "18:47"},
            {"20", "05:30", "12:15", "14:54", "17:19", "18:45"}, {"25", "05:34", "12:16", "14:53", "17:17", "18:44"},
            {"30", "05:38", "12:18", "14:52", "17:16", "18:43"}};
    private static final Prayer[] CMP = {Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA};
    private static final double SB_LAT = 35.0382, SB_LNG = 9.4849;

    private static int maxDiff(boolean tuned, double lat, double lng, Prayer only) {
        int max = 0;
        for (String[] r : TABLE) {
            LocalDate d = LocalDate.of(2026, 11, Integer.parseInt(r[0]));
            DayTimes t = tuned ? PrayerCalc.computeTunisia(d, lat, lng, TUN) : PrayerCalc.compute(d, lat, lng, TUN);
            for (int i = 0; i < 5; i++) {
                if (only != null && CMP[i] != only) continue;
                max = Math.max(max, Math.abs(t.minutesOf(CMP[i]) - AladhanParser.parseHm(r[i + 1])));
            }
        }
        return max;
    }

    @Test public void rawAsrMatchesTheTableExactly() { assertEquals(0, maxDiff(false, SB_LAT, SB_LNG, Prayer.ASR)); }
    @Test public void rawAnglesDifferFromTableByMoreThanTwoMinutes() { assertTrue(maxDiff(false, SB_LAT, SB_LNG, null) > 2); }
    @Test public void tunedEstimateIsWithinOneMinuteOfTheTable() { assertTrue(maxDiff(true, SB_LAT, SB_LNG, null) <= 1); }
    @Test public void tunedEstimateAtMenzelBouzaianeIsWithinTwoMinutesOfTheCityTable() {
        assertTrue(maxDiff(true, PrayerLocation.LAT, PrayerLocation.LNG, null) <= 2);
    }
    @Test public void estimateIsMarkedEstimatedAndStrictlyOrdered() {
        DayTimes d = PrayerCalc.computeTunisia(LocalDate.of(2026, 7, 1), PrayerLocation.LAT, PrayerLocation.LNG, TUN);
        assertEquals(DayTimes.Source.ESTIMATED, d.source);
        for (int i = 1; i < 6; i++) assertTrue(d.minutes[i] > d.minutes[i - 1]);
    }
    @Test public void summerTimesAreLaterAndSunriseIsEarlier() {
        DayTimes w = PrayerCalc.computeTunisia(LocalDate.of(2026, 12, 21), PrayerLocation.LAT, PrayerLocation.LNG, TUN);
        DayTimes s = PrayerCalc.computeTunisia(LocalDate.of(2026, 6, 21), PrayerLocation.LAT, PrayerLocation.LNG, TUN);
        assertTrue(s.minutesOf(Prayer.ISHA) > w.minutesOf(Prayer.ISHA));
        assertTrue(s.minutesOf(Prayer.SUNRISE) < w.minutesOf(Prayer.SUNRISE));
    }

    // ---- API parser
    private static final String SAMPLE = "{\"code\":200,\"status\":\"OK\",\"data\":["
            + "{\"timings\":{\"Fajr\":\"05:15 (CET)\",\"Sunrise\":\"06:40 (CET)\",\"Dhuhr\":\"12:13 (CET)\",\"Asr\":\"15:05 (CET)\","
            + "\"Sunset\":\"17:28 (CET)\",\"Maghrib\":\"17:33 (CET)\",\"Isha\":\"18:56 (CET)\",\"Imsak\":\"05:05 (CET)\"},"
            + "\"date\":{\"readable\":\"01 Nov 2026\",\"gregorian\":{\"date\":\"01-11-2026\"},\"hijri\":{\"date\":\"x\"}}},"
            + "{\"timings\":{\"Fajr\":\"05:15 (CET)\",\"Sunrise\":\"06:41 (CET)\",\"Dhuhr\":\"12:13 (CET)\",\"Asr\":\"15:05 (CET)\","
            + "\"Maghrib\":\"17:32 (CET)\",\"Isha\":\"18:55 (CET)\"},"
            + "\"date\":{\"gregorian\":{ \"date\" : \"02-11-2026\"}}}]}";

    @Test public void parsesCalendarResponse() {
        List<DayTimes> l = AladhanParser.parse(SAMPLE);
        assertNotNull(l);
        assertEquals(2, l.size());
        assertEquals(LocalDate.of(2026, 11, 1), l.get(0).date);
        assertEquals(5 * 60 + 15, l.get(0).minutesOf(Prayer.FAJR));
        assertEquals(17 * 60 + 33, l.get(0).minutesOf(Prayer.MAGHRIB));
        assertEquals(LocalDate.of(2026, 11, 2), l.get(1).date);
        assertEquals(DayTimes.Source.API, l.get(0).source);
    }
    @Test public void rejectsBadResponses() {
        assertNull(AladhanParser.parse(null));
        assertNull(AladhanParser.parse("{}"));
        assertNull(AladhanParser.parse("<html>blocked</html>"));
        assertNull(AladhanParser.parse(SAMPLE.replace("\"Isha\":\"18:56 (CET)\"", "\"Isha\":\"soon\"")));
        assertNull(AladhanParser.parse(SAMPLE.replace("01-11-2026", "32-13-2026")));
        assertNull(AladhanParser.parse(SAMPLE.replace("\"Fajr\":\"05:15 (CET)\",\"Sunrise\":\"06:40", "\"Fajr\":\"09:15 (CET)\",\"Sunrise\":\"06:40")));
    }
    @Test public void parseHm() {
        assertEquals(315, AladhanParser.parseHm("05:15"));
        assertEquals(315, AladhanParser.parseHm("05:15 (CET)"));
        assertEquals(-1, AladhanParser.parseHm("25:00"));
        assertEquals(-1, AladhanParser.parseHm("5:15"));
        assertEquals(-1, AladhanParser.parseHm(null));
    }

    // ---- offsets
    @Test public void offsetsAreClampedToTenMinutes() {
        DayTimes d = new DayTimes(LocalDate.of(2026, 11, 1), new int[]{300, 400, 700, 900, 1000, 1100}, DayTimes.Source.API);
        DayTimes o = d.withOffsets(new int[]{-99, 0, 3, 0, 99, -2});
        assertEquals(290, o.minutesOf(Prayer.FAJR));
        assertEquals(703, o.minutesOf(Prayer.DHUHR));
        assertEquals(1010, o.minutesOf(Prayer.MAGHRIB));
        assertEquals(1098, o.minutesOf(Prayer.ISHA));
        assertEquals(d.minutesOf(Prayer.FAJR), 300); // original untouched
    }

    // ---- next prayer
    private static DayTimes day(LocalDate dt) {
        return new DayTimes(dt, new int[]{315, 400, 733, 899, 1045, 1130}, DayTimes.Source.API); // 05:15 06:40 12:13 14:59 17:25 18:50
    }
    private static ZonedDateTime t(int d, int h, int m) { return ZonedDateTime.of(2026, 11, d, h, m, 0, 0, TUN); }

    @Test public void beforeFajrNextIsFajrAndNoCurrent() {
        NextPrayer.Result r = NextPrayer.compute(day(LocalDate.of(2026, 11, 10)), day(LocalDate.of(2026, 11, 11)), t(10, 3, 0));
        assertNull(r.current); assertEquals(Prayer.FAJR, r.next); assertEquals(t(10, 5, 15), r.nextAt);
    }
    @Test public void afterFajrBeforeSunriseCurrentIsFajr() {
        NextPrayer.Result r = NextPrayer.compute(day(LocalDate.of(2026, 11, 10)), day(LocalDate.of(2026, 11, 11)), t(10, 6, 0));
        assertEquals(Prayer.FAJR, r.current); assertEquals(Prayer.DHUHR, r.next);
    }
    @Test public void betweenSunriseAndDhuhrNoCurrent() {
        NextPrayer.Result r = NextPrayer.compute(day(LocalDate.of(2026, 11, 10)), day(LocalDate.of(2026, 11, 11)), t(10, 9, 0));
        assertNull(r.current); assertEquals(Prayer.DHUHR, r.next);
    }
    @Test public void exactlyAtAdhanThatPrayerIsCurrent() {
        NextPrayer.Result r = NextPrayer.compute(day(LocalDate.of(2026, 11, 10)), day(LocalDate.of(2026, 11, 11)), t(10, 12, 13));
        assertEquals(Prayer.DHUHR, r.current); assertEquals(Prayer.ASR, r.next);
    }
    @Test public void afterIshaNextIsTomorrowFajrAndMonthRolloverWorks() {
        NextPrayer.Result r = NextPrayer.compute(day(LocalDate.of(2026, 11, 30)), day(LocalDate.of(2026, 12, 1)), t(30, 22, 0));
        assertEquals(Prayer.ISHA, r.current); assertEquals(Prayer.FAJR, r.next);
        assertEquals(ZonedDateTime.of(2026, 12, 1, 5, 15, 0, 0, TUN), r.nextAt);
    }
    @Test public void prayerWindowOpensAtAdhanForTenMinutes() {
        DayTimes d = day(LocalDate.of(2026, 11, 10));
        assertNull(NextPrayer.windowEnd(d, t(10, 12, 12), 10));
        assertEquals(t(10, 12, 23), NextPrayer.windowEnd(d, t(10, 12, 13), 10));
        assertEquals(t(10, 12, 23), NextPrayer.windowEnd(d, t(10, 12, 22), 10));
        assertNull(NextPrayer.windowEnd(d, t(10, 12, 23), 10));
        assertNull(NextPrayer.windowEnd(d, t(10, 6, 40), 10)); // sunrise is not a prayer
    }

    // ---- qibla
    @Test public void qiblaBearings() {
        assertEquals(119.0, QiblaMath.bearing(51.5074, -0.1278), 0.6);   // London ~119
        assertEquals(58.5, QiblaMath.bearing(40.7128, -74.0060), 0.6);   // New York ~58.5
        assertEquals(295.0, QiblaMath.bearing(-6.2, 106.85), 1.0);       // Jakarta ~295
        assertEquals(0.0, QiblaMath.bearing(0.0, QiblaMath.KAABA_LNG), 0.01);
        double tn = QiblaMath.bearing(PrayerLocation.LAT, PrayerLocation.LNG);
        assertTrue("Tunisia qibla is south-east (" + tn + ")", tn > 100 && tn < 125);
    }
    @Test public void turnNeededIsShortestRotation() {
        assertEquals(20, QiblaMath.turnNeeded(10, 350), 1e-9);
        assertEquals(-20, QiblaMath.turnNeeded(350, 10), 1e-9);
        assertEquals(0, QiblaMath.turnNeeded(110, 110), 1e-9);
    }

    // ---- weekly summary
    @Test public void weekSummaryCounts() {
        WeekSummary w = new WeekSummary();
        for (String s : new String[]{"on_time", "on_time", "late", "missed", "garbage"}) w.add(s);
        assertEquals(2, w.onTime); assertEquals(1, w.late); assertEquals(1, w.missed);
        assertEquals(3, w.prayed()); assertEquals(4, w.logged());
    }
}

package com.ahmed2027.app.prayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/** Cache (SQLite) + Aladhan fetch + offline estimate. Network calls must run off the main thread. */
public final class PrayerRepo {
    public enum Result { OK, OFFLINE, ERROR }

    private PrayerRepo() {}

    /** Times for a date with the user's manual offsets: cached API data, else a labelled estimate. */
    public static DayTimes get(Context c, LocalDate date) {
        DayTimes d = cached(c, date);
        if (d == null) d = PrayerCalc.computeTunisia(date, PrayerLocation.LAT, PrayerLocation.LNG, PrayerLocation.ZONE);
        return d.withOffsets(PrayerSettings.offsets(c));
    }

    private static DayTimes cached(Context c, LocalDate date) {
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery(
                "SELECT fajr, sunrise, dhuhr, asr, maghrib, isha FROM prayer_times WHERE date=?",
                new String[]{date.toString()});
        try {
            if (!cur.moveToFirst()) return null;
            int[] m = new int[6];
            for (int i = 0; i < 6; i++) m[i] = cur.getInt(i);
            return new DayTimes(date, m, DayTimes.Source.API);
        } finally { cur.close(); }
    }

    private static int cachedDaysIn(Context c, YearMonth ym) {
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM prayer_times WHERE date>=? AND date<=?",
                new String[]{ym.atDay(1).toString(), ym.atEndOfMonth().toString()});
        try { return cur.moveToFirst() ? cur.getInt(0) : 0; } finally { cur.close(); }
    }

    /** True when today's times come from the cache (not the estimate). */
    public static boolean hasCachedToday(Context c) { return cached(c, LocalDate.now(PrayerLocation.ZONE)) != null; }

    /**
     * Fetches this month and next month if they are not fully cached; keeps only those 2 months.
     * Safe to call often. Never throws.
     */
    public static Result refresh(Context c, boolean force) {
        try {
            YearMonth now = YearMonth.now(PrayerLocation.ZONE);
            Result res = Result.OK;
            for (YearMonth ym : new YearMonth[]{now, now.plusMonths(1)}) {
                if (!force && cachedDaysIn(c, ym) >= ym.lengthOfMonth()) continue;
                String json;
                try { json = httpGet(url(ym)); }
                catch (java.net.UnknownHostException | java.net.SocketTimeoutException | java.net.ConnectException e) {
                    return Result.OFFLINE;
                }
                List<DayTimes> days = AladhanParser.parse(json);
                if (days == null || days.isEmpty()) return Result.ERROR;
                store(c, days);
                res = Result.OK;
            }
            prune(c, now);
            PrayerSettings.setLastRefreshWall(c, Clock.wall());
            return res;
        } catch (IOException | RuntimeException e) {
            return Result.ERROR;
        }
    }

    static String url(YearMonth ym) {
        return "https://api.aladhan.com/v1/calendar?latitude=" + PrayerLocation.LAT
                + "&longitude=" + PrayerLocation.LNG + "&method=" + PrayerLocation.API_METHOD
                + "&month=" + ym.getMonthValue() + "&year=" + ym.getYear();
    }

    private static String httpGet(String u) throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) new URL(u).openConnection();
        con.setConnectTimeout(10_000);
        con.setReadTimeout(10_000);
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + con.getResponseCode());
            InputStream in = con.getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192]; int n;
            while ((n = r.read(buf)) > 0) {
                sb.append(buf, 0, n);
                if (sb.length() > 1_000_000) throw new IOException("response too large");
            }
            return sb.toString();
        } finally { con.disconnect(); }
    }

    private static void store(Context c, List<DayTimes> days) {
        SQLiteDatabase db = Db.get(c).getWritableDatabase();
        db.beginTransaction();
        try {
            for (DayTimes d : days) {
                ContentValues v = new ContentValues();
                v.put("date", d.date.toString());
                v.put("fajr", d.minutes[0]); v.put("sunrise", d.minutes[1]); v.put("dhuhr", d.minutes[2]);
                v.put("asr", d.minutes[3]); v.put("maghrib", d.minutes[4]); v.put("isha", d.minutes[5]);
                v.put("source", "api"); v.put("fetched_wall", Clock.wall());
                db.insertWithOnConflict("prayer_times", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static void prune(Context c, YearMonth current) {
        Db.get(c).getWritableDatabase().delete("prayer_times", "date<?", new String[]{current.atDay(1).toString()});
    }
}

package com.ahmed2027.app.prayer;

import android.content.Context;
import android.content.SharedPreferences;

import com.ahmed2027.app.core.Prefs;

/** Prayer preferences. Everything that does something on its own is OFF until the user turns it on. */
public final class PrayerSettings {
    public static final String SOUND_DEFAULT = "default", SOUND_ALARM = "alarm", SOUND_SILENT = "silent";
    private PrayerSettings() {}

    private static SharedPreferences p(Context c) { return Prefs.get(c); }

    public static boolean remindersOn(Context c) { return p(c).getBoolean("pr_reminders", false); }
    public static void setRemindersOn(Context c, boolean on) { p(c).edit().putBoolean("pr_reminders", on).apply(); }

    public static boolean focusWindowOn(Context c) { return p(c).getBoolean("pr_focus", false); }
    public static void setFocusWindowOn(Context c, boolean on) { p(c).edit().putBoolean("pr_focus", on).apply(); }

    public static String sound(Context c) { return p(c).getString("pr_sound", SOUND_DEFAULT); }
    public static void setSound(Context c, String s) { p(c).edit().putString("pr_sound", s).apply(); }

    /** Manual offset per Prayer.ordinal(), -10..+10 minutes. */
    public static int offset(Context c, Prayer pr) { return p(c).getInt("pr_off_" + pr.ordinal(), 0); }
    public static void setOffset(Context c, Prayer pr, int minutes) {
        p(c).edit().putInt("pr_off_" + pr.ordinal(), Math.max(-10, Math.min(10, minutes))).apply();
    }
    public static int[] offsets(Context c) {
        int[] o = new int[Prayer.values().length];
        for (Prayer pr : Prayer.values()) o[pr.ordinal()] = offset(c, pr);
        return o;
    }

    public static long lastRefreshWall(Context c) { return p(c).getLong("pr_last_refresh", 0); }
    public static void setLastRefreshWall(Context c, long w) { p(c).edit().putLong("pr_last_refresh", w).apply(); }
}

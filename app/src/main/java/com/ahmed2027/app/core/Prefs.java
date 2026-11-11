package com.ahmed2027.app.core;

import android.content.Context;
import android.content.SharedPreferences;

/** Single SharedPreferences file for app settings. */
public final class Prefs {
    private static final String FILE = "ahmed2027_prefs";
    public static final String KEY_LANG = "lang";

    public static final String KEY_TEXT_SCALE = "text_scale";
    public static final String KEY_ONBOARDED = "onboarded";
    public static final String KEY_INTENTION = "intention";
    public static final String KEY_INTENTION_DATE = "intention_date";

    /** Text size steps: small, normal, large, extra large. */
    public static final float[] TEXT_SCALES = {0.9f, 1.0f, 1.15f, 1.3f};

    private Prefs() {}

    public static int getTextScaleIndex(Context c) {
        int i = get(c).getInt(KEY_TEXT_SCALE, 1);
        return i < 0 || i >= TEXT_SCALES.length ? 1 : i;
    }

    public static void setTextScaleIndex(Context c, int i) {
        if (i < 0 || i >= TEXT_SCALES.length) i = 1;
        get(c).edit().putInt(KEY_TEXT_SCALE, i).apply();
    }

    public static boolean isOnboarded(Context c) { return get(c).getBoolean(KEY_ONBOARDED, false); }

    public static void setOnboarded(Context c) { get(c).edit().putBoolean(KEY_ONBOARDED, true).apply(); }

    /** Intention for the given local date (yyyy-MM-dd), or "" if none set for that day. */
    public static String getIntention(Context c, String date) {
        SharedPreferences p = get(c);
        return date.equals(p.getString(KEY_INTENTION_DATE, "")) ? p.getString(KEY_INTENTION, "") : "";
    }

    public static void setIntention(Context c, String date, String text) {
        get(c).edit().putString(KEY_INTENTION, text).putString(KEY_INTENTION_DATE, date).apply();
    }

    public static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String getLang(Context c) {
        return Lang.normalize(get(c).getString(KEY_LANG, Lang.DEFAULT));
    }

    public static void setLang(Context c, String code) {
        get(c).edit().putString(KEY_LANG, Lang.normalize(code)).apply();
    }
}

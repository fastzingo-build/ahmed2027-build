package com.ahmed2027.app.prayer;

import android.content.Context;

import com.ahmed2027.app.R;

public final class PrayerNames {
    private static final int[] RES = {R.string.pr_fajr, R.string.pr_sunrise, R.string.pr_dhuhr,
            R.string.pr_asr, R.string.pr_maghrib, R.string.pr_isha};
    private PrayerNames() {}
    public static String label(Context c, Prayer p) { return c.getString(RES[p.ordinal()]); }
    public static String key(Prayer p) { return p.name().toLowerCase(java.util.Locale.ROOT); }
}

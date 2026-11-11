package com.ahmed2027.app.shield;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import com.ahmed2027.app.core.Prefs;

/** Shield on/off preference and whether the accessibility service is actually enabled. */
public final class ShieldStatus {
    private static final String KEY_ON = "shield_on";
    private ShieldStatus() {}

    public static boolean isOn(Context c) { return Prefs.get(c).getBoolean(KEY_ON, true); }

    public static void setOn(Context c, boolean on) { Prefs.get(c).edit().putBoolean(KEY_ON, on).apply(); }

    public static boolean serviceEnabled(Context c) {
        String flat = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(flat)) return false;
        String me = new ComponentName(c, ShieldService.class).flattenToString();
        String meShort = new ComponentName(c, ShieldService.class).flattenToShortString();
        for (String part : flat.split(":")) {
            if (part.equalsIgnoreCase(me) || part.equalsIgnoreCase(meShort)) return true;
        }
        return false;
    }
}

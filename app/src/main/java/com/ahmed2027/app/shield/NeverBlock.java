package com.ahmed2027.app.shield;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.provider.Telephony;
import android.telecom.TelecomManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Packages the Shield must NEVER interfere with: emergency, phone/dialer, SMS, Settings, Clock/alarms,
 * Maps. Known names plus whatever this device resolves for those actions (OEM apps differ).
 */
public final class NeverBlock {
    private static final String[] STATIC = {
            "com.android.dialer", "com.google.android.dialer", "com.android.phone", "com.android.server.telecom",
            "com.android.incallui", "com.android.contacts", "com.android.emergency",
            "com.android.mms", "com.google.android.apps.messaging", "com.android.messaging",
            "com.android.settings", "com.android.deskclock", "com.google.android.deskclock",
            "com.google.android.apps.maps", "com.android.systemui", "com.android.packageinstaller",
            "com.google.android.permissioncontroller", "com.android.permissioncontroller"
    };

    private NeverBlock() {}

    public static Set<String> resolve(Context c) {
        Set<String> s = new HashSet<>(Arrays.asList(STATIC));
        add(s, c, new Intent(Intent.ACTION_DIAL));
        add(s, c, new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")));
        add(s, c, new Intent(Settings.ACTION_SETTINGS));
        add(s, c, new Intent(AlarmClock.ACTION_SHOW_ALARMS));
        add(s, c, new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0")));
        try {
            TelecomManager tm = (TelecomManager) c.getSystemService(Context.TELECOM_SERVICE);
            if (tm != null && tm.getDefaultDialerPackage() != null) s.add(tm.getDefaultDialerPackage());
        } catch (RuntimeException ignored) { /* keep static list */ }
        try {
            String sms = Telephony.Sms.getDefaultSmsPackage(c);
            if (sms != null) s.add(sms);
        } catch (RuntimeException ignored) { /* keep static list */ }
        return s;
    }

    private static void add(Set<String> s, Context c, Intent i) {
        try {
            List<ResolveInfo> l = c.getPackageManager().queryIntentActivities(i, 0);
            for (ResolveInfo ri : l) s.add(ri.activityInfo.packageName);
        } catch (RuntimeException ignored) { /* keep what we have */ }
    }
}

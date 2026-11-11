package com.ahmed2027.app.focus;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

/**
 * Do Not Disturb through NotificationManager policy access (explicit grant by the user in system settings).
 * No NotificationListenerService; notification content is never read. Allowed through: calls from starred contacts,
 * alarms, and this app's prayer channels (bypass DND). System emergency alerts are never suppressed by DND.
 * Two independent reasons (focus session, quiet period) can hold DND on; the previous setting is restored when both end.
 */
public final class FocusMode {
    public static final String REASON_SESSION = "session", REASON_QUIET = "quiet";
    private static final String PREFS = "focus_mode";
    private FocusMode() {}

    private static SharedPreferences p(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    private static NotificationManager nm(Context c) { return (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE); }

    public static boolean hasAccess(Context c) { return nm(c).isNotificationPolicyAccessGranted(); }
    public static Intent accessIntent() { return new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS); }

    /** @return false when DND access is not granted (the caller shows the honest fallback). */
    public static boolean engage(Context c, String reason) {
        if (!hasAccess(c)) return false;
        NotificationManager n = nm(c);
        SharedPreferences sp = p(c);
        boolean already = sp.getBoolean(REASON_SESSION, false) || sp.getBoolean(REASON_QUIET, false);
        if (!already) sp.edit().putInt("previous", n.getCurrentInterruptionFilter()).apply();
        n.setNotificationPolicy(new NotificationManager.Policy(
                NotificationManager.Policy.PRIORITY_CATEGORY_CALLS | NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS,
                NotificationManager.Policy.PRIORITY_SENDERS_STARRED, NotificationManager.Policy.PRIORITY_SENDERS_STARRED));
        for (NotificationChannel ch : n.getNotificationChannels()) {
            if (ch.getId().startsWith("prayer_") && !ch.canBypassDnd()) { ch.setBypassDnd(true); n.createNotificationChannel(ch); }
        }
        n.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
        sp.edit().putBoolean(reason, true).apply();
        return true;
    }

    public static void release(Context c, String reason) {
        SharedPreferences sp = p(c);
        sp.edit().putBoolean(reason, false).apply();
        boolean still = sp.getBoolean(REASON_SESSION, false) || sp.getBoolean(REASON_QUIET, false);
        if (still || !hasAccess(c)) return;
        nm(c).setInterruptionFilter(sp.getInt("previous", NotificationManager.INTERRUPTION_FILTER_ALL));
    }

    public static boolean isHeld(Context c, String reason) { return p(c).getBoolean(reason, false); }
}

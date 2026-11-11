package com.ahmed2027.app.prayer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Schedules ONE alarm: the next adhan. When it fires, PrayerReceiver notifies and schedules the next.
 * Android 12: exact alarms need the user's SCHEDULE_EXACT_ALARM grant; without it we fall back to an
 * inexact alarm (may be minutes late) and the settings screen says so.
 */
public final class PrayerAlarms {
    public static final String ACTION_ADHAN = "com.ahmed2027.app.ADHAN";
    private static final int REQ = 7001;

    private PrayerAlarms() {}

    public static boolean canExact(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return am != null && (android.os.Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
    }

    private static PendingIntent pi(Context c, Prayer p, int flags) {
        Intent i = new Intent(c, PrayerReceiver.class).setAction(ACTION_ADHAN)
                .putExtra("prayer", p == null ? -1 : p.ordinal());
        return PendingIntent.getBroadcast(c, REQ, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi(c, null, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    /** (Re)schedules the next adhan; no-op (and cancels) when reminders are off. Safe to call anytime. */
    public static void scheduleNext(Context c) {
        Context app = c.getApplicationContext();
        if (!PrayerSettings.remindersOn(app)) { cancel(app); return; }
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PrayerProvider.Next n = PrayerProvider.next(app);
        PendingIntent p = pi(app, n.prayer, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            if (canExact(app)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, n.timeWall, p);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, n.timeWall, p);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, n.timeWall, p); // grant revoked meanwhile
        }
    }
}

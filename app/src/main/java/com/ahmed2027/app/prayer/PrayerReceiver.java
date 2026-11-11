package com.ahmed2027.app.prayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Adhan alarm + re-scheduling triggers: BOOT_COMPLETED, TIMEZONE_CHANGED, TIME_SET, package replaced.
 * Contains no secrets and reads no notification content.
 */
public class PrayerReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        String a = i == null ? null : i.getAction();
        if (a == null) return;
        if (PrayerAlarms.ACTION_ADHAN.equals(a)) {
            int ord = i.getIntExtra("prayer", -1);
            Prayer[] all = Prayer.values();
            if (ord >= 0 && ord < all.length && PrayerSettings.remindersOn(c)) {
                PrayerNotifier.notifyAdhan(c, all[ord]);
            }
        }
        // For every trigger (adhan fired, reboot, timezone/time change, update): plan the next one.
        PrayerAlarms.scheduleNext(c);
    }
}

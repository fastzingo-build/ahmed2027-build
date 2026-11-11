package com.ahmed2027.app.prayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

import com.ahmed2027.app.MainActivity;
import com.ahmed2027.app.R;

/** Notification channels "Prayer": one per sound choice (a channel's sound cannot change after creation). */
public final class PrayerNotifier {
    private PrayerNotifier() {}

    private static String channelId(String sound) { return "prayer_" + sound; }

    public static void ensureChannels(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        String name = c.getString(R.string.pr_channel_name);
        make(nm, PrayerSettings.SOUND_DEFAULT, name, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.USAGE_NOTIFICATION);
        make(nm, PrayerSettings.SOUND_ALARM, name + " (2)", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), AudioAttributes.USAGE_ALARM);
        make(nm, PrayerSettings.SOUND_SILENT, name + " (3)", null, AudioAttributes.USAGE_NOTIFICATION);
    }

    private static void make(NotificationManager nm, String sound, String name, Uri uri, int usage) {
        NotificationChannel ch = new NotificationChannel(channelId(sound), name, NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription(name);
        if (uri == null) { ch.setSound(null, null); ch.enableVibration(true); }
        else ch.setSound(uri, new AudioAttributes.Builder().setUsage(usage).build());
        nm.createNotificationChannel(ch);
    }

    public static void notifyAdhan(Context c, Prayer p) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || !nm.areNotificationsEnabled()) return;
        ensureChannels(c);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 7002, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String label = PrayerNames.label(c, p);
        Notification n = new Notification.Builder(c, channelId(PrayerSettings.sound(c)))
                .setSmallIcon(R.drawable.ic_stat_prayer)
                .setContentTitle(c.getString(R.string.pr_notif_title, label))
                .setContentText(c.getString(R.string.pr_notif_text))
                .setContentIntent(pi).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER)
                .build();
        nm.notify(7100 + p.ordinal(), n);
    }
}

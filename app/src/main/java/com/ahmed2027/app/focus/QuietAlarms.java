package com.ahmed2027.app.focus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Items;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Stored quiet periods (items type "quiet", status on/off) and the alarm that flips DND at the boundaries. */
public final class QuietAlarms {
    public static final String TYPE = "quiet";
    private QuietAlarms() {}

    public static List<QuietPeriods.Period> enabled(Context c) {
        List<QuietPeriods.Period> out = new ArrayList<QuietPeriods.Period>();
        for (Items.Item it : Items.list(c, TYPE, "on", 50)) {
            QuietPeriods.Period p = QuietPeriods.parse(it.title);
            if (p != null) out.add(p);
        }
        return out;
    }

    /** Applies the current state and schedules the next boundary. Safe to call at any time. */
    public static void apply(Context c) {
        List<QuietPeriods.Period> ps = enabled(c);
        LocalTime now = LocalTime.now(Clock.zone());
        int nowMin = now.getHour() * 60 + now.getMinute();
        if (ps.isEmpty()) { FocusMode.release(c, FocusMode.REASON_QUIET); }
        else if (QuietPeriods.inAny(ps, nowMin)) { FocusMode.engage(c, FocusMode.REASON_QUIET); }
        else { FocusMode.release(c, FocusMode.REASON_QUIET); }
        schedule(c, ps, nowMin, now.getSecond());
    }

    private static PendingIntent pi(Context c) {
        return PendingIntent.getBroadcast(c, 7001, new Intent(c, QuietReceiver.class).setAction("com.ahmed2027.app.QUIET_TICK"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void schedule(Context c, List<QuietPeriods.Period> ps, int nowMin, int sec) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi(c));
        int m = QuietPeriods.minutesToNextBoundary(ps, nowMin);
        if (m < 0) return;
        long delay = Math.max(30_000L, m * 60_000L - sec * 1000L);
        // Inexact on purpose: no exact-alarm permission is needed; a boundary may be a few minutes late.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Clock.wall() + delay, pi(c));
    }
}

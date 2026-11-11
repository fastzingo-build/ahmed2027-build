package com.ahmed2027.app.growth;

import android.content.Context;
import android.database.Cursor;

import com.ahmed2027.app.core.ActivityLog;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Db;
import com.ahmed2027.app.core.Items;

import java.time.LocalDate;

/** Gathers the weekly Growth Score inputs from local data (rolling 7 days; previous = the 7 days before). */
public final class ScoreInputs {
    private ScoreInputs() {}

    public static GrowthScore.Input week(Context c, int weeksAgo) {
        LocalDate end = LocalDate.now(Clock.zone()).minusDays(7L * weeksAgo);
        LocalDate start = end.minusDays(6);
        GrowthScore.Input in = new GrowthScore.Input();

        int onTime = 0, late = 0;
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery(
                "SELECT state, COUNT(*) FROM prayer_log WHERE local_date>=? AND local_date<=? GROUP BY state",
                new String[]{start.toString(), end.toString()});
        try {
            while (cur.moveToNext()) {
                if ("on_time".equals(cur.getString(0))) onTime = cur.getInt(1);
                else if ("late".equals(cur.getString(0))) late = cur.getInt(1);
            }
        } finally { cur.close(); }
        in.prayer = (onTime + late == 0 && weeksAgo > 0) ? null : GrowthScore.prayerPct(onTime, late);

        int habits = Items.count(c, HabitStore.TYPE, "active");
        in.habits = GrowthScore.habitsPct(HabitStore.doneBetween(c, start, end), habits);

        int learn = ActivityLog.minutes(c, ActivityLog.LEARN, start, end);
        int move = ActivityLog.minutes(c, ActivityLog.MOVE, start, end);
        in.learning = (weeksAgo > 0 && learn == 0) ? null : GrowthScore.pct(learn, GrowthScore.LEARN_TARGET_MIN);
        in.movement = (weeksAgo > 0 && move == 0) ? null : GrowthScore.pct(move, GrowthScore.MOVE_TARGET_MIN);
        // Creation and focus only count once the user has actually used those features; otherwise excluded (null), never faked.
        if (Items.count(c, "content", null) > 0) {
            int pub = 0;
            for (Items.Item it : Items.query(c, "content", "published", "id ASC", 1000)) {
                String d = it.meta().get("pub", "");
                if (d.compareTo(start.toString()) >= 0 && d.compareTo(end.toString()) <= 0 && !d.isEmpty()) pub++;
            }
            int goal = c.getSharedPreferences("creator", Context.MODE_PRIVATE).getInt("goal", 3);
            in.creation = (weeksAgo > 0 && pub == 0) ? null : GrowthScore.pct(pub, goal);
        }
        int focusEver = ActivityLog.minutes(c, ActivityLog.FOCUS, LocalDate.of(2000, 1, 1), end);
        if (focusEver > 0) {
            int f = ActivityLog.minutes(c, ActivityLog.FOCUS, start, end);
            in.focus = (weeksAgo > 0 && f == 0) ? null : GrowthScore.pct(f, GrowthScore.FOCUS_TARGET_MIN);
        }
        return in;
    }
}

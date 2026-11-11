package com.ahmed2027.app.core;

import android.content.Context;
import android.database.Cursor;

import java.time.LocalDate;

/** Minutes of learning / movement / focus / creation per local day (items type "activity"). */
public final class ActivityLog {
    public static final String LEARN = "learn", MOVE = "move", FOCUS = "focus", CREATE = "create";
    private ActivityLog() {}

    public static String today() { return LocalDate.now(Clock.zone()).toString(); }

    public static void log(Context c, String kind, int minutes) {
        if (minutes <= 0) return;
        Items.addFull(c, "activity", kind, String.valueOf(minutes), today(), 0, null);
    }

    /** Sum of minutes for kind between two local dates, inclusive. */
    public static int minutes(Context c, String kind, LocalDate from, LocalDate to) {
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(CAST(body AS INTEGER)),0) FROM items WHERE type='activity' AND title=? AND status>=? AND status<=?",
                new String[]{kind, from.toString(), to.toString()});
        try { return cur.moveToFirst() ? cur.getInt(0) : 0; } finally { cur.close(); }
    }

    public static int lastDays(Context c, String kind, int days) {
        LocalDate t = LocalDate.now(Clock.zone());
        return minutes(c, kind, t.minusDays(days - 1), t);
    }
}

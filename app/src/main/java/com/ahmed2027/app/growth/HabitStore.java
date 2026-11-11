package com.ahmed2027.app.growth;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ahmed2027.app.core.Db;
import com.ahmed2027.app.core.Items;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Habits = items(type habit, status active|archived, meta routine=morning|evening|anytime) + habit_log. */
public final class HabitStore {
    public static final String TYPE = "habit";
    private HabitStore() {}

    public static List<Items.Item> active(Context c) { return Items.list(c, TYPE, "active", 200); }

    public static long add(Context c, String title, String routine) {
        return Items.addFull(c, TYPE, title, null, "active", 0, "routine=" + routine);
    }

    public static void archive(Context c, long id) { Items.setStatus(c, id, "archived"); }

    public static void setDone(Context c, long habitId, LocalDate d, boolean done) {
        SQLiteDatabase db = Db.get(c).getWritableDatabase();
        if (done) {
            ContentValues v = new ContentValues();
            v.put("habit_id", habitId); v.put("local_date", d.toString()); v.put("done", 1);
            db.insertWithOnConflict("habit_log", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        } else {
            db.delete("habit_log", "habit_id=? AND local_date=?", new String[]{String.valueOf(habitId), d.toString()});
        }
    }

    public static Set<LocalDate> doneDates(Context c, long habitId) {
        Set<LocalDate> s = new HashSet<>();
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery(
                "SELECT local_date FROM habit_log WHERE habit_id=? AND done=1", new String[]{String.valueOf(habitId)});
        try { while (cur.moveToNext()) s.add(LocalDate.parse(cur.getString(0))); } finally { cur.close(); }
        return s;
    }

    /** Total done check-ins for active habits between two dates inclusive. */
    public static int doneBetween(Context c, LocalDate from, LocalDate to) {
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM habit_log l JOIN items i ON i.id=l.habit_id AND i.type='habit' AND i.status='active' "
                        + "WHERE l.done=1 AND l.local_date>=? AND l.local_date<=?",
                new String[]{from.toString(), to.toString()});
        try { return cur.moveToFirst() ? cur.getInt(0) : 0; } finally { cur.close(); }
    }
}

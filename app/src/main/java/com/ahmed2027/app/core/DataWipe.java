package com.ahmed2027.app.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * "Delete my data": removes everything the user created (items, habit log, prayer log, cached prayer times).
 * Deliberately KEPT: social_sessions and tamper_log (otherwise deleting data would reset the daily social quota),
 * app classification (a protection setting) and the Friend Key hash.
 */
public final class DataWipe {
    private DataWipe() {}

    public static void wipe(Context c) {
        SQLiteDatabase db = Db.get(c).getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("items", "type<>?", new String[]{"tamper_log"});
            db.delete("habit_log", null, null);
            db.delete("prayer_log", null, null);
            db.delete("prayer_times", null, null);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        for (String f : new String[]{"work", "creator", "focus_mode"}) c.getSharedPreferences(f, Context.MODE_PRIVATE).edit().clear().apply();
        com.ahmed2027.app.focus.FocusMode.release(c, com.ahmed2027.app.focus.FocusMode.REASON_SESSION);
        com.ahmed2027.app.focus.FocusMode.release(c, com.ahmed2027.app.focus.FocusMode.REASON_QUIET);
        com.ahmed2027.app.focus.QuietAlarms.apply(c);
    }
}

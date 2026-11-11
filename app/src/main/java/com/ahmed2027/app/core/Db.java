package com.ahmed2027.app.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Single SQLite database. Generic `items` table for most modules, plus dedicated
 * tables only where the master context requires them.
 * Migrations: add a case in onUpgrade for every VERSION bump; never edit old cases.
 */
public final class Db extends SQLiteOpenHelper {
    public static final String NAME = "ahmed2027.db";
    public static final int VERSION = 2;

    private static Db instance;

    public static synchronized Db get(Context c) {
        if (instance == null) instance = new Db(c.getApplicationContext());
        return instance;
    }

    private Db(Context c) {
        super(c, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createV1(db);
        onUpgrade(db, 1, VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createV2(db);
        // if (oldVersion < 3) { ... }
    }

    /** v2: cached prayer times (minutes since local midnight), one row per date. */
    private static void createV2(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE prayer_times ("
                + "date TEXT PRIMARY KEY,"
                + "fajr INTEGER NOT NULL, sunrise INTEGER NOT NULL, dhuhr INTEGER NOT NULL,"
                + "asr INTEGER NOT NULL, maghrib INTEGER NOT NULL, isha INTEGER NOT NULL,"
                + "source TEXT NOT NULL, fetched_wall INTEGER NOT NULL)");
    }

    private static void createV1(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "type TEXT NOT NULL,"
                + "title TEXT,"
                + "body TEXT,"
                + "status TEXT,"
                + "due_ts INTEGER,"
                + "meta_json TEXT,"
                + "created_ts INTEGER NOT NULL,"
                + "updated_ts INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_items_type_status ON items(type, status)");
        db.execSQL("CREATE INDEX idx_items_due ON items(due_ts)");

        // One session per package per local date (enforced by UNIQUE).
        db.execSQL("CREATE TABLE social_sessions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "package TEXT NOT NULL,"
                + "local_date TEXT NOT NULL,"
                + "start_wall INTEGER NOT NULL,"
                + "start_elapsed INTEGER NOT NULL,"
                + "expiry_wall INTEGER NOT NULL,"
                + "UNIQUE(package, local_date))");

        db.execSQL("CREATE TABLE prayer_log ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "local_date TEXT NOT NULL,"
                + "prayer TEXT NOT NULL,"
                + "state TEXT NOT NULL,"      // on_time | late | missed
                + "logged_wall INTEGER NOT NULL,"
                + "UNIQUE(local_date, prayer))");

        // class: allowed | limited | blocked | hidden
        db.execSQL("CREATE TABLE app_class ("
                + "package TEXT PRIMARY KEY,"
                + "class TEXT NOT NULL,"
                + "favorite INTEGER NOT NULL DEFAULT 0,"
                + "updated_wall INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE habit_log ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "habit_id INTEGER NOT NULL,"
                + "local_date TEXT NOT NULL,"
                + "done INTEGER NOT NULL DEFAULT 1,"
                + "UNIQUE(habit_id, local_date))");
    }
}

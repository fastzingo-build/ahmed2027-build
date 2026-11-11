package com.ahmed2027.app.social;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Db;
import com.ahmed2027.app.core.Items;
import com.ahmed2027.app.core.Prefs;

import java.time.DateTimeException;
import java.time.ZoneId;

/** SQLite-backed store. The `package` column holds the quota key (tiktok / instagram). */
public final class SqlSessionStore implements SocialSessionManager.Store {
    private static final String KEY_HW = "social_high_water";
    private static final String KEY_ZONE = "home_zone";
    private final Context c;

    public SqlSessionStore(Context c) { this.c = c.getApplicationContext(); }

    /** Fixed home zone: persisted on first use so a later device timezone change cannot start a new day. */
    public static ZoneId homeZone(Context c) {
        String id = Prefs.get(c).getString(KEY_ZONE, null);
        if (id != null) {
            try { return ZoneId.of(id); } catch (DateTimeException ignored) { /* fall through */ }
        }
        ZoneId z = Clock.zone();
        Prefs.get(c).edit().putString(KEY_ZONE, z.getId()).apply();
        return z;
    }

    public static SocialSessionManager manager(Context c) {
        return new SocialSessionManager(new SqlSessionStore(c), homeZone(c));
    }

    @Override public SocialSessionManager.Session find(String key, String localDate) {
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery(
                "SELECT start_wall, start_elapsed, expiry_wall FROM social_sessions WHERE package=? AND local_date=?",
                new String[]{key, localDate});
        try {
            if (!cur.moveToFirst()) return null;
            return new SocialSessionManager.Session(key, localDate, cur.getLong(0), cur.getLong(1), cur.getLong(2));
        } finally { cur.close(); }
    }

    @Override public boolean insert(SocialSessionManager.Session s) {
        ContentValues v = new ContentValues();
        v.put("package", s.key); v.put("local_date", s.localDate);
        v.put("start_wall", s.startWall); v.put("start_elapsed", s.startElapsed); v.put("expiry_wall", s.expiryWall);
        SQLiteDatabase db = Db.get(c).getWritableDatabase();
        return db.insertWithOnConflict("social_sessions", null, v, SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }

    @Override public long highWater() { return Prefs.get(c).getLong(KEY_HW, 0L); }

    @Override public void setHighWater(long wall) { Prefs.get(c).edit().putLong(KEY_HW, wall).apply(); }

    @Override public void log(String event) { Items.add(c, "tamper_log", event, "logged"); }
}

package com.ahmed2027.app.launcher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Db;

import java.util.HashMap;
import java.util.Map;

/** app_class table access. NOTE: becomes Friend-Key protected in Phase 4. */
public final class AppClassStore {
    public static final class Entry {
        public final String cls;
        public final boolean favorite;
        Entry(String cls, boolean favorite) { this.cls = cls; this.favorite = favorite; }
    }

    private AppClassStore() {}

    public static Map<String, Entry> all(Context c) {
        Map<String, Entry> m = new HashMap<>();
        Cursor cur = Db.get(c).getReadableDatabase()
                .rawQuery("SELECT package, class, favorite FROM app_class", null);
        try {
            while (cur.moveToNext()) {
                m.put(cur.getString(0), new Entry(AppClass.normalize(cur.getString(1)), cur.getInt(2) != 0));
            }
        } finally { cur.close(); }
        return m;
    }

    public static void setClass(Context c, String pkg, String cls) {
        upsert(c, pkg, AppClass.normalize(cls), null);
    }

    public static void setFavorite(Context c, String pkg, boolean fav) {
        upsert(c, pkg, null, fav);
    }

    private static void upsert(Context c, String pkg, String cls, Boolean fav) {
        SQLiteDatabase db = Db.get(c).getWritableDatabase();
        Cursor q = db.rawQuery("SELECT class, favorite FROM app_class WHERE package=?", new String[]{pkg});
        String oldCls = AppClass.ALLOWED; int oldFav = 0;
        try { if (q.moveToFirst()) { oldCls = AppClass.normalize(q.getString(0)); oldFav = q.getInt(1); } }
        finally { q.close(); }
        ContentValues v = new ContentValues();
        v.put("package", pkg);
        v.put("class", cls != null ? cls : oldCls);
        v.put("favorite", fav != null ? (fav ? 1 : 0) : oldFav);
        v.put("updated_wall", Clock.wall());
        db.insertWithOnConflict("app_class", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }
}

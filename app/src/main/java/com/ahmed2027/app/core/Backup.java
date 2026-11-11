package com.ahmed2027.app.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON export/import. Restores items, app_class, habit_log, prayer_log and a few prefs.
 * social_sessions is exported for the user's records but NEVER restored, so a restore
 * cannot be used to reset a daily quota.
 */
public final class Backup {
    public static final int FORMAT = 1;
    private static final Map<String, List<String>> COLS = new HashMap<>();
    static {
        COLS.put("items", Arrays.asList("id", "type", "title", "body", "status", "due_ts", "meta_json", "created_ts", "updated_ts"));
        COLS.put("app_class", Arrays.asList("package", "class", "favorite", "updated_wall"));
        COLS.put("habit_log", Arrays.asList("id", "habit_id", "local_date", "done"));
        COLS.put("prayer_log", Arrays.asList("id", "local_date", "prayer", "state", "logged_wall"));
        COLS.put("social_sessions", Arrays.asList("id", "package", "local_date", "start_wall", "start_elapsed", "expiry_wall"));
    }
    private static final String[] RESTORABLE = {"items", "app_class", "habit_log", "prayer_log"};
    private static final String[] EXPORTED = {"items", "app_class", "habit_log", "prayer_log", "social_sessions"};

    private Backup() {}

    public static String export(Context c) throws JSONException {
        SQLiteDatabase db = Db.get(c).getReadableDatabase();
        JSONObject root = new JSONObject();
        root.put("app", "ahmed2027");
        root.put("format", FORMAT);
        root.put("exported_wall", Clock.wall());
        JSONObject tables = new JSONObject();
        for (String t : EXPORTED) {
            JSONArray rows = new JSONArray();
            Cursor cur = db.rawQuery("SELECT * FROM " + t, null);
            try {
                while (cur.moveToNext()) {
                    JSONObject r = new JSONObject();
                    for (int i = 0; i < cur.getColumnCount(); i++) {
                        String n = cur.getColumnName(i);
                        switch (cur.getType(i)) {
                            case Cursor.FIELD_TYPE_INTEGER: r.put(n, cur.getLong(i)); break;
                            case Cursor.FIELD_TYPE_FLOAT: r.put(n, cur.getDouble(i)); break;
                            case Cursor.FIELD_TYPE_STRING: r.put(n, cur.getString(i)); break;
                            default: break; // null: omit
                        }
                    }
                    rows.put(r);
                }
            } finally { cur.close(); }
            tables.put(t, rows);
        }
        root.put("tables", tables);
        JSONObject prefs = new JSONObject();
        prefs.put("lang", Prefs.getLang(c));
        prefs.put("text_scale", Prefs.getTextScaleIndex(c));
        root.put("prefs", prefs);
        return root.toString(2);
    }

    /** Validates fully first, then replaces data in one transaction. Returns rows restored. */
    public static int restore(Context c, String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (!"ahmed2027".equals(root.optString("app")) || root.optInt("format", -1) != FORMAT) {
            throw new JSONException("not an AHMED 2027 backup");
        }
        JSONObject tables = root.getJSONObject("tables");
        Map<String, List<ContentValues>> parsed = new HashMap<>();
        for (String t : RESTORABLE) {
            JSONArray rows = tables.optJSONArray(t);
            if (rows == null) continue;
            List<String> allowed = COLS.get(t);
            List<ContentValues> list = new java.util.ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject r = rows.getJSONObject(i);
                ContentValues v = new ContentValues();
                Iterator<String> it = r.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    if (!allowed.contains(k)) throw new JSONException("unknown column " + t + "." + k);
                    Object o = r.get(k);
                    if (o instanceof Number) {
                        double d = ((Number) o).doubleValue();
                        if (d == Math.rint(d)) v.put(k, ((Number) o).longValue()); else v.put(k, d);
                    } else if (o instanceof String) {
                        v.put(k, (String) o);
                    } else throw new JSONException("bad value " + t + "." + k);
                }
                list.add(v);
            }
            parsed.put(t, list);
        }
        int n = 0;
        SQLiteDatabase db = Db.get(c).getWritableDatabase();
        db.beginTransaction();
        try {
            for (String t : RESTORABLE) {
                List<ContentValues> rows = parsed.get(t);
                if (rows == null) continue;
                db.delete(t, null, null);
                for (ContentValues v : rows) { db.insertOrThrow(t, null, v); n++; }
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        JSONObject prefs = root.optJSONObject("prefs");
        if (prefs != null) {
            Prefs.setLang(c, prefs.optString("lang", Lang.DEFAULT));
            Prefs.setTextScaleIndex(c, prefs.optInt("text_scale", 1));
        }
        return n;
    }
}

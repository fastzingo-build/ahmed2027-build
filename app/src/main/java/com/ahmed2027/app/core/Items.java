package com.ahmed2027.app.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/** Access to the generic items table (tasks, habits, notes, vocab, goals, learn resources, logs...). */
public final class Items {
    public static final class Item {
        public final long id; public final String type, title, body, status, meta;
        public final long dueTs, createdTs;
        Item(long id, String type, String title, String body, String status, long dueTs, String meta, long createdTs) {
            this.id = id; this.type = type; this.title = title == null ? "" : title; this.body = body == null ? "" : body;
            this.status = status == null ? "" : status; this.dueTs = dueTs; this.meta = meta == null ? "" : meta; this.createdTs = createdTs;
        }
        public Meta meta() { return Meta.parse(meta); }
    }

    private static final String COLS = "id, type, title, body, status, COALESCE(due_ts,0), meta_json, created_ts";

    private Items() {}

    private static Item read(Cursor c) {
        return new Item(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getLong(5), c.getString(6), c.getLong(7));
    }

    public static long add(Context c, String type, String title, String status) {
        return addFull(c, type, title, null, status, 0, null);
    }

    public static long addFull(Context c, String type, String title, String body, String status, long dueTs, String meta) {
        long now = Clock.wall();
        ContentValues v = new ContentValues();
        v.put("type", type); v.put("title", title); v.put("body", body); v.put("status", status);
        if (dueTs != 0) v.put("due_ts", dueTs);
        v.put("meta_json", meta);
        v.put("created_ts", now); v.put("updated_ts", now);
        return Db.get(c).getWritableDatabase().insert("items", null, v);
    }

    /** status == null matches any status. orderBy is a trusted constant, e.g. "created_ts ASC". */
    public static List<Item> query(Context c, String type, String status, String orderBy, int limit) {
        List<Item> out = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM items WHERE type=?" + (status == null ? "" : " AND status=?")
                + " ORDER BY " + orderBy + " LIMIT " + limit;
        String[] args = status == null ? new String[]{type} : new String[]{type, status};
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery(sql, args);
        try { while (cur.moveToNext()) out.add(read(cur)); } finally { cur.close(); }
        return out;
    }

    public static List<Item> list(Context c, String type, String status, int limit) {
        return query(c, type, status, "created_ts ASC", limit);
    }

    public static Item get(Context c, long id) {
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery("SELECT " + COLS + " FROM items WHERE id=?", new String[]{String.valueOf(id)});
        try { return cur.moveToFirst() ? read(cur) : null; } finally { cur.close(); }
    }

    public static int count(Context c, String type, String status) {
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM items WHERE type=?" + (status == null ? "" : " AND status=?"),
                status == null ? new String[]{type} : new String[]{type, status});
        try { return cur.moveToFirst() ? cur.getInt(0) : 0; } finally { cur.close(); }
    }

    public static void setStatus(Context c, long id, String status) {
        ContentValues v = new ContentValues();
        v.put("status", status); v.put("updated_ts", Clock.wall());
        Db.get(c).getWritableDatabase().update("items", v, "id=?", new String[]{String.valueOf(id)});
    }

    public static void setMeta(Context c, long id, Meta meta) {
        ContentValues v = new ContentValues();
        v.put("meta_json", meta.toString()); v.put("updated_ts", Clock.wall());
        Db.get(c).getWritableDatabase().update("items", v, "id=?", new String[]{String.valueOf(id)});
    }

    public static void setBody(Context c, long id, String body) {
        ContentValues v = new ContentValues();
        v.put("body", body); v.put("updated_ts", Clock.wall());
        Db.get(c).getWritableDatabase().update("items", v, "id=?", new String[]{String.valueOf(id)});
    }

    public static void setDue(Context c, long id, long dueTs) {
        ContentValues v = new ContentValues();
        v.put("due_ts", dueTs); v.put("updated_ts", Clock.wall());
        Db.get(c).getWritableDatabase().update("items", v, "id=?", new String[]{String.valueOf(id)});
    }

    public static void delete(Context c, long id) {
        SQLiteDatabase db = Db.get(c).getWritableDatabase();
        db.delete("items", "id=?", new String[]{String.valueOf(id)});
    }
}

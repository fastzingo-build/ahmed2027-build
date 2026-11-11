package com.ahmed2027.app.analytics;

import android.content.Context;
import android.database.Cursor;

import com.ahmed2027.app.core.Db;
import com.ahmed2027.app.core.Items;

import java.util.HashMap;
import java.util.Map;

/** Local aggregation only. Nothing leaves the device. */
public final class AnalyticsData {
    private AnalyticsData() {}

    private static Map<String, Integer> run(Context c, String sql, String... args) {
        Map<String, Integer> m = new HashMap<String, Integer>();
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery(sql, args);
        try { while (cur.moveToNext()) m.put(cur.getString(0), cur.getInt(1)); } finally { cur.close(); }
        return m;
    }

    public static Map<String, Integer> habits(Context c) {
        return run(c, "SELECT local_date, COUNT(*) FROM habit_log WHERE done=1 GROUP BY local_date");
    }

    public static Map<String, Integer> minutes(Context c, String kind) {
        return run(c, "SELECT status, SUM(CAST(body AS INTEGER)) FROM items WHERE type='activity' AND title=? GROUP BY status", kind);
    }

    public static Map<String, Integer> socialSessions(Context c) {
        return run(c, "SELECT local_date, COUNT(*) FROM social_sessions GROUP BY local_date");
    }

    public static Map<String, Integer> published(Context c) {
        Map<String, Integer> m = new HashMap<String, Integer>();
        for (Items.Item it : Items.query(c, "content", "published", "id ASC", 1000)) {
            String d = it.meta().get("pub", "");
            if (d.isEmpty()) continue;
            Integer v = m.get(d);
            m.put(d, v == null ? 1 : v + 1);
        }
        return m;
    }

    /** {onTime, logged} for prayers between two ISO dates inclusive. */
    public static int[] prayers(Context c, String from, String to) {
        int on = 0, all = 0;
        Cursor cur = Db.get(c).getReadableDatabase().rawQuery(
                "SELECT state, COUNT(*) FROM prayer_log WHERE local_date>=? AND local_date<=? GROUP BY state", new String[]{from, to});
        try {
            while (cur.moveToNext()) {
                String s = cur.getString(0);
                if ("on_time".equals(s)) { on += cur.getInt(1); all += cur.getInt(1); }
                else if ("late".equals(s) || "missed".equals(s)) all += cur.getInt(1);
            }
        } finally { cur.close(); }
        return new int[]{on, all};
    }
}

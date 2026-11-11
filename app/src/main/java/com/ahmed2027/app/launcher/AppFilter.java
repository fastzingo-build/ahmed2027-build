package com.ahmed2027.app.launcher;

import java.util.Locale;

/** Pure filtering rules for the drawer (unit-tested). chip: null = "All". */
public final class AppFilter {
    private AppFilter() {}

    public static boolean matches(String label, String pkg, String cls, String chip, String query) {
        cls = AppClass.normalize(cls);
        if (chip == null) {
            if (AppClass.HIDDEN.equals(cls)) return false; // hidden only under its own chip
        } else if (!chip.equals(cls)) {
            return false;
        }
        if (query == null) return true;
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return true;
        return (label != null && label.toLowerCase(Locale.ROOT).contains(q))
                || (pkg != null && pkg.toLowerCase(Locale.ROOT).contains(q));
    }
}

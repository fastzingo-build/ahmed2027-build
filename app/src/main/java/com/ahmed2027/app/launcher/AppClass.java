package com.ahmed2027.app.launcher;

/** Classification values stored in app_class.class. */
public final class AppClass {
    public static final String ALLOWED = "allowed";
    public static final String LIMITED = "limited";
    public static final String BLOCKED = "blocked";
    public static final String HIDDEN = "hidden";
    public static final String[] ALL = {ALLOWED, LIMITED, BLOCKED, HIDDEN};

    private AppClass() {}

    public static String normalize(String c) {
        for (String s : ALL) if (s.equals(c)) return s;
        return ALLOWED;
    }
}

package com.ahmed2027.app.friendkey;

import com.ahmed2027.app.launcher.AppClass;

/** Which actions need the Friend Key. */
public final class Protection {
    /**
     * DECISION FOR THE OWNER: false = only changes that make an app LESS restricted need the key
     * (so you can always add a restriction yourself, but need your friend to remove one).
     * true = every class change needs the key (literal reading of prompt 04).
     */
    public static final boolean GATE_ALL_CLASS_CHANGES = false;

    private Protection() {}

    private static int rank(String cls) {
        String c = AppClass.normalize(cls);
        if (AppClass.BLOCKED.equals(c)) return 2;
        if (AppClass.LIMITED.equals(c)) return 1;
        return 0; // allowed, hidden (hidden is not enforcement)
    }

    public static boolean classChangeNeedsKey(String oldCls, String newCls) {
        String o = AppClass.normalize(oldCls), n = AppClass.normalize(newCls);
        if (o.equals(n)) return false;
        if (GATE_ALL_CLASS_CHANGES) return true;
        return rank(n) < rank(o);
    }
}

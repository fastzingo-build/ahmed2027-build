package com.ahmed2027.app.shield;

import android.content.Context;

import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Prefs;

/** "Continue for 10 minutes" pass for LIMITED apps, persisted (survives process death). */
public final class LimitedPass {
    public static final long PASS_MS = 10L * 60_000L;
    private LimitedPass() {}

    public static void grant(Context c, String pkg) {
        Prefs.get(c).edit().putLong("pass_" + pkg, Clock.wall() + PASS_MS).apply();
    }

    public static boolean valid(Context c, String pkg) {
        long exp = Prefs.get(c).getLong("pass_" + pkg, 0L);
        long now = Clock.wall();
        // A pass can never be longer than PASS_MS: a far-future value means the clock was set back.
        return exp > now && exp - now <= PASS_MS + 60_000L;
    }
}

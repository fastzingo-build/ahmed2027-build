package com.ahmed2027.app.friendkey;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Items;

/** Convenience API for "is this unlocked?" and "ask for the key". */
public final class FriendKeyGate {
    public static final int REQ_KEY = 200;

    private FriendKeyGate() {}

    public static UnlockManager unlocks(Context c) { return new UnlockManager(new FriendKeyStore(c)); }

    public static boolean active(Context c, String scope, String target) {
        return unlocks(c).isActive(scope, target, Clock.wall(), Clock.elapsed());
    }

    public static long remaining(Context c, String scope, String target) {
        return unlocks(c).remainingMillis(scope, target, Clock.wall(), Clock.elapsed());
    }

    /** Opens the Friend Key screen. Result RESULT_OK = unlocked for the scope's duration. */
    public static void request(Activity a, String scope, String target) {
        a.startActivityForResult(intent(a, scope, target), REQ_KEY);
    }

    public static Intent intent(Context c, String scope, String target) {
        return new Intent(c, FriendKeyActivity.class)
                .putExtra(FriendKeyActivity.EXTRA_SCOPE, scope)
                .putExtra(FriendKeyActivity.EXTRA_TARGET, target);
    }

    /** Local log: time (created_ts) + scope + target. NEVER the key. */
    public static void logUnlock(Context c, String scope, String target) {
        Items.add(c, "unlock_log", "scope=" + scope + (target == null ? "" : " target=" + target), "logged");
    }

    public static void logWrong(Context c, int failures) {
        Items.add(c, "unlock_log", "wrong_attempt count=" + failures, "logged");
    }
}

package com.ahmed2027.app.friendkey;

/**
 * Time-limited unlocks granted after a correct Friend Key. Auto re-lock = they simply expire.
 * Scopes: APP (one app, 15 min), SETTINGS (protected settings, 10 min), CLASSIFY (change app classes, 10 min).
 */
public final class UnlockManager {
    public static final String APP = "APP", SETTINGS = "SETTINGS", CLASSIFY = "CLASSIFY";
    public static final long MIN = 60_000L;
    private static final long SLACK = 60_000L;

    public interface Store { String get(String key); void put(String key, String value); void remove(String key); }

    private final Store store;

    public UnlockManager(Store store) { this.store = store; }

    public static long durationFor(String scope) {
        return APP.equals(scope) ? 15 * MIN : 10 * MIN;
    }

    private static String k(String scope, String target) { return scope + "|" + (target == null ? "" : target); }

    public void grant(String scope, String target, long nowWall, long nowElapsed) {
        long d = durationFor(scope);
        store.put(k(scope, target), nowWall + ":" + nowElapsed + ":" + d);
    }

    public void revoke(String scope, String target) { store.remove(k(scope, target)); }

    /** Remaining unlocked milliseconds, 0 if locked. Uses the shorter of wall / elapsed views. */
    public long remainingMillis(String scope, String target, long nowWall, long nowElapsed) {
        String v = store.get(k(scope, target));
        if (v == null) return 0;
        String[] p = v.split(":");
        if (p.length != 3) return 0;
        long startWall, startElapsed, dur;
        try { startWall = Long.parseLong(p[0]); startElapsed = Long.parseLong(p[1]); dur = Long.parseLong(p[2]); }
        catch (NumberFormatException e) { return 0; }
        long byWall = startWall + dur - nowWall;
        if (byWall > dur + SLACK) return 0; // clock moved back past the grant: never extend
        long rem = byWall;
        if (nowElapsed >= startElapsed) rem = Math.min(rem, dur - (nowElapsed - startElapsed));
        return Math.max(0, rem);
    }

    public boolean isActive(String scope, String target, long nowWall, long nowElapsed) {
        return remainingMillis(scope, target, nowWall, nowElapsed) > 0;
    }
}

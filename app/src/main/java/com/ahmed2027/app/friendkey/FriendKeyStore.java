package com.ahmed2027.app.friendkey;

import android.content.Context;
import android.content.SharedPreferences;

import com.ahmed2027.app.core.Prefs;

/** SharedPreferences persistence for the limiter, the unlocks and the cooling-off start. Never stores the key. */
public final class FriendKeyStore implements AttemptLimiter.Store, UnlockManager.Store {
    private final SharedPreferences p;

    public FriendKeyStore(Context c) { this.p = Prefs.get(c.getApplicationContext()); }

    @Override public AttemptLimiter.State load() {
        AttemptLimiter.State s = new AttemptLimiter.State();
        s.failures = p.getInt("fk_fail", 0);
        s.lockUntilWall = p.getLong("fk_lock_until", 0);
        s.lockDelayMs = p.getLong("fk_lock_delay", 0);
        s.lockStartElapsed = p.getLong("fk_lock_el", 0);
        s.highWater = p.getLong("fk_hw", 0);
        return s;
    }

    /** commit(): synchronous, so a process kill right after a wrong attempt cannot lose it. */
    @Override public void save(AttemptLimiter.State s) {
        p.edit().putInt("fk_fail", s.failures).putLong("fk_lock_until", s.lockUntilWall)
                .putLong("fk_lock_delay", s.lockDelayMs).putLong("fk_lock_el", s.lockStartElapsed)
                .putLong("fk_hw", s.highWater).commit();
    }

    @Override public String get(String key) { return p.getString("fk_unlock_" + key, null); }
    @Override public void put(String key, String value) { p.edit().putString("fk_unlock_" + key, value).commit(); }
    @Override public void remove(String key) { p.edit().remove("fk_unlock_" + key).commit(); }

    // cooling-off
    public boolean coolingEnabled() { return p.getBoolean("fk_cool_on", true); }
    public void setCoolingEnabled(boolean on) { p.edit().putBoolean("fk_cool_on", on).commit(); }
    public boolean coolStarted() { return p.getLong("fk_cool_wall", 0) != 0; }
    public long coolWall() { return p.getLong("fk_cool_wall", 0); }
    public long coolElapsed() { return p.getLong("fk_cool_el", 0); }
    public void startCooling(long wall, long elapsed) { p.edit().putLong("fk_cool_wall", wall).putLong("fk_cool_el", elapsed).commit(); }
    public void clearCooling() { p.edit().remove("fk_cool_wall").remove("fk_cool_el").commit(); }
}

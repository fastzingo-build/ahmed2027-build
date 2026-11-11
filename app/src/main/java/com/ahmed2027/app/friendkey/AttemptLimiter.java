package com.ahmed2027.app.friendkey;

/**
 * Escalating delay after wrong keys: 3 wrong -> 1 min, 4 -> 5 min, 5 -> 15 min, 6+ -> 60 min.
 * State is persisted by the Store so it survives process death. Only the Friend Key screen is
 * affected; Android itself is never locked.
 */
public final class AttemptLimiter {
    public static final long MIN = 60_000L;
    private static final long[] DELAYS = {1 * MIN, 5 * MIN, 15 * MIN, 60 * MIN};

    public static final class State {
        public int failures;
        public long lockUntilWall, lockDelayMs, lockStartElapsed, highWater;
    }

    public interface Store { State load(); void save(State s); }

    private final Store store;

    public AttemptLimiter(Store store) { this.store = store; }

    public static long delayForFailures(int failures) {
        if (failures < 3) return 0;
        return DELAYS[Math.min(failures - 3, DELAYS.length - 1)];
    }

    private long eff(State s, long nowWall) {
        long e = Math.max(nowWall, s.highWater);
        if (e > s.highWater) { s.highWater = e; store.save(s); }
        return e;
    }

    /** Milliseconds until another attempt is allowed (0 = allowed). Stricter of wall / elapsed. */
    public long remainingMillis(long nowWall, long nowElapsed) {
        State s = store.load();
        if (s.lockDelayMs <= 0) return 0;
        long e = eff(s, nowWall);
        long byWall = s.lockUntilWall - e;
        long best = byWall;
        if (nowElapsed >= s.lockStartElapsed) {
            best = Math.max(best, s.lockDelayMs - (nowElapsed - s.lockStartElapsed));
        }
        return Math.max(0, best);
    }

    public boolean canAttempt(long nowWall, long nowElapsed) { return remainingMillis(nowWall, nowElapsed) == 0; }

    /** @return delay now imposed (0 for the first two failures). */
    public long recordFailure(long nowWall, long nowElapsed) {
        State s = store.load();
        long e = eff(s, nowWall);
        s.failures++;
        long d = delayForFailures(s.failures);
        s.lockDelayMs = d;
        s.lockUntilWall = d == 0 ? 0 : e + d;
        s.lockStartElapsed = nowElapsed;
        store.save(s);
        return d;
    }

    public void recordSuccess() {
        State s = store.load();
        s.failures = 0; s.lockDelayMs = 0; s.lockUntilWall = 0; s.lockStartElapsed = 0;
        store.save(s);
    }

    public int failures() { return store.load().failures; }
}

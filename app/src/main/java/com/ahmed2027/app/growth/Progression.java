package com.ahmed2027.app.growth;

/** Gentle progression: a suggested weekly target is never more than +10 % over what was actually done. Pure. */
public final class Progression {
    public static final int START_MINUTES = 15;
    private Progression() {}

    /** Largest allowed target given last week's actual amount (rounded up). */
    public static int cap(int lastWeek) { return (int) Math.ceil(lastWeek * 1.10); }

    /** Suggested target for the coming week. Never forced: it is only a suggestion. */
    public static int suggest(int lastWeek) {
        return lastWeek <= 0 ? START_MINUTES : Math.max(lastWeek, cap(lastWeek));
    }

    /** Applies the +10 % limit to a user-chosen target. */
    public static int limit(int lastWeek, int chosen) {
        return lastWeek <= 0 ? Math.min(chosen, START_MINUTES) : Math.min(chosen, cap(lastWeek));
    }
}

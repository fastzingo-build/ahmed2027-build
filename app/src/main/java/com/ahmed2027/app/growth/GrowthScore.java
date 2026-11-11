package com.ahmed2027.app.growth;

/**
 * Weekly Growth Score from six components, each 0..100 or null when there is no data source yet
 * (creation and focus arrive in Phase 7). The score is the mean of the components that exist.
 * Only a weekly trend is meant to be shown, never a daily number. Pure.
 */
public final class GrowthScore {
    public static final int PRAYERS_PER_WEEK = 35, LEARN_TARGET_MIN = 150, MOVE_TARGET_MIN = 90, FOCUS_TARGET_MIN = 150;

    public static final class Input {
        public Integer prayer, habits, learning, movement, creation, focus;
        public Integer[] all() { return new Integer[]{prayer, habits, learning, movement, creation, focus}; }
    }

    private GrowthScore() {}

    public static int pct(int value, int target) {
        if (target <= 0 || value <= 0) return 0;
        return (int) Math.min(100, Math.round(100.0 * value / target));
    }

    /** On-time prayers count fully, late ones 70 %. */
    public static int prayerPct(int onTime, int late) {
        return (int) Math.min(100, Math.round(100.0 * (onTime + 0.7 * late) / PRAYERS_PER_WEEK));
    }

    /** null when the user has no habits yet. */
    public static Integer habitsPct(int doneDays, int habitCount) {
        return habitCount <= 0 ? null : pct(doneDays, habitCount * 7);
    }

    /** Mean of the available components, or null when none is available. */
    public static Integer score(Input in) {
        int n = 0, sum = 0;
        for (Integer v : in.all()) if (v != null) { n++; sum += v; }
        return n == 0 ? null : (int) Math.round(sum / (double) n);
    }

    /** Signed change vs the previous week, or null when either week has no score. */
    public static Integer trend(Integer now, Integer prev) {
        return now == null || prev == null ? null : now - prev;
    }
}

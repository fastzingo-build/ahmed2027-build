package com.ahmed2027.app.growth;

/** Varied rotation with rest days. Pure. */
public final class FitnessPlan {
    public static final int REST = -1;
    private FitnessPlan() {}

    /**
     * @param active3 whether each of the last three days (oldest first) had a workout
     * @return REST after three active days in a row, otherwise a rotating session index
     */
    public static int suggest(boolean[] active3, long epochDay, int sessionCount) {
        if (sessionCount <= 0) return REST;
        if (active3 != null && active3.length == 3 && active3[0] && active3[1] && active3[2]) return REST;
        return (int) Math.floorMod(epochDay, (long) sessionCount);
    }
}

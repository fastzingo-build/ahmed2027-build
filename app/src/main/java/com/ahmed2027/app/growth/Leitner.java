package com.ahmed2027.app.growth;

/** Leitner boxes 1..5 with review intervals of 1, 2, 4, 8 and 16 days. Pure. */
public final class Leitner {
    public static final int[] INTERVAL_DAYS = {1, 2, 4, 8, 16};
    public static final int MAX_BOX = 5;

    private Leitner() {}

    public static int nextBox(int box, boolean correct) {
        if (!correct) return 1;
        return Math.min(MAX_BOX, Math.max(1, box) + 1);
    }

    /** Due day (epoch day) after reviewing into newBox. */
    public static long dueDay(long todayEpochDay, int newBox) {
        int b = Math.max(1, Math.min(MAX_BOX, newBox));
        return todayEpochDay + INTERVAL_DAYS[b - 1];
    }

    public static boolean isDue(long dueEpochDay, long todayEpochDay) { return dueEpochDay <= todayEpochDay; }
}

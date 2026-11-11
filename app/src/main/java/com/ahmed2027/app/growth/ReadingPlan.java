package com.ahmed2027.app.growth;

/** Pages-per-day reading plan arithmetic. Pure. */
public final class ReadingPlan {
    private ReadingPlan() {}

    public static int remaining(int totalPages, int pagesRead) { return Math.max(0, totalPages - Math.max(0, pagesRead)); }

    /** Days needed at pagesPerDay (0 if finished; -1 if the plan has no daily pace). */
    public static int daysToFinish(int totalPages, int pagesRead, int pagesPerDay) {
        int rem = remaining(totalPages, pagesRead);
        if (rem == 0) return 0;
        if (pagesPerDay <= 0) return -1;
        return (rem + pagesPerDay - 1) / pagesPerDay;
    }

    public static int percent(int totalPages, int pagesRead) {
        if (totalPages <= 0) return 0;
        return (int) Math.min(100, Math.round(100.0 * Math.max(0, pagesRead) / totalPages));
    }
}

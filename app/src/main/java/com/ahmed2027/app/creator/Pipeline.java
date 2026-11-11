package com.ahmed2027.app.creator;

import java.time.LocalDate;
import java.util.List;

/** Content pipeline Idea → Planning → Recording → Editing → Ready → Published. Pure. */
public final class Pipeline {
    public static final String[] STAGES = {"idea", "planning", "recording", "editing", "ready", "published"};
    private Pipeline() {}

    public static int index(String stage) {
        for (int i = 0; i < STAGES.length; i++) if (STAGES[i].equals(stage)) return i;
        return 0;
    }

    public static String next(String stage) { int i = index(stage); return STAGES[Math.min(STAGES.length - 1, i + 1)]; }
    public static boolean isLast(String stage) { return index(stage) == STAGES.length - 1; }

    /** Number of ISO dates in [weekStart, weekStart+6]. */
    public static int inWeek(List<String> isoDates, LocalDate weekStart) {
        int n = 0;
        for (String s : isoDates) {
            try {
                LocalDate d = LocalDate.parse(s);
                if (!d.isBefore(weekStart) && !d.isAfter(weekStart.plusDays(6))) n++;
            } catch (Exception ignored) { }
        }
        return n;
    }

    public static LocalDate weekStart(LocalDate d) { return d.minusDays((d.getDayOfWeek().getValue() + 6) % 7); }
}

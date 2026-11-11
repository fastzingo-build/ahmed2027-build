package com.ahmed2027.app.growth;

import java.time.LocalDate;
import java.util.Set;

/** Habit logic with "restart gently" semantics: a missed day never shows a broken streak. Pure. */
public final class HabitMath {
    private HabitMath() {}

    /** Consecutive done days ending today (or yesterday if today is not done yet). 0 = no current run. */
    public static int run(Set<LocalDate> done, LocalDate today) {
        LocalDate d = done.contains(today) ? today : today.minusDays(1);
        int n = 0;
        while (done.contains(d)) { n++; d = d.minusDays(1); }
        return n;
    }

    /** Done today after at least two days away (but not the very first time ever): a comeback. */
    public static boolean comeback(Set<LocalDate> done, LocalDate today) {
        if (!done.contains(today) || done.contains(today.minusDays(1)) || done.contains(today.minusDays(2))) return false;
        for (LocalDate d : done) if (d.isBefore(today.minusDays(2))) return true;
        return false;
    }

    /** Days done within the 7 days ending today. */
    public static int weekDone(Set<LocalDate> done, LocalDate today) {
        int n = 0;
        for (int i = 0; i < 7; i++) if (done.contains(today.minusDays(i))) n++;
        return n;
    }
}

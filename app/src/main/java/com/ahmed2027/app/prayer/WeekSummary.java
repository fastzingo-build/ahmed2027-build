package com.ahmed2027.app.prayer;

/** Non-shaming weekly counts from prayer_log states ("on_time", "late", "missed"). Pure. */
public final class WeekSummary {
    public int onTime, late, missed;

    public void add(String state) {
        if ("on_time".equals(state)) onTime++;
        else if ("late".equals(state)) late++;
        else if ("missed".equals(state)) missed++;
    }

    /** Prayers marked as prayed (on time or late). */
    public int prayed() { return onTime + late; }

    public int logged() { return onTime + late + missed; }
}

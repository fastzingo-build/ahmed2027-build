package com.ahmed2027.app.prayer;

/** The six timings shown to the user. Only the five with isPrayer() are prayers. */
public enum Prayer {
    FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA;

    public boolean isPrayer() { return this != SUNRISE; }
}

package com.ahmed2027.app.core;

import android.os.SystemClock;

import java.time.ZoneId;

/** Thin Android wrapper: wall clock + monotonic clock. Logic lives in ClockMath. */
public final class Clock {
    private Clock() {}

    public static long wall() { return System.currentTimeMillis(); }

    public static long elapsed() { return SystemClock.elapsedRealtime(); }

    public static ZoneId zone() { return ZoneId.systemDefault(); }
}

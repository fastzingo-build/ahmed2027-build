package com.ahmed2027.app.core;

/** Pure-Java language rules (no Android deps, unit-testable). */
public final class Lang {
    public static final String AR = "ar";
    public static final String EN = "en";
    public static final String DEFAULT = AR;

    private Lang() {}

    /** Returns a supported code; anything unknown falls back to the default (Arabic). */
    public static String normalize(String code) {
        if (EN.equals(code)) return EN;
        if (AR.equals(code)) return AR;
        return DEFAULT;
    }

    public static boolean isRtl(String code) {
        return AR.equals(normalize(code));
    }
}

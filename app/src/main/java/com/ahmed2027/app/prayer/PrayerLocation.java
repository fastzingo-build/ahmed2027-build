package com.ahmed2027.app.prayer;

import java.time.ZoneId;

/**
 * Menzel Bouzaiane, Sidi Bouzid governorate, Tunisia.
 * 34.574 N, 9.429 E per https://en.wikipedia.org/wiki/Menzel_Bouzaiane (34 deg 34'26" N, 9 deg 25'44" E).
 * NOTE: the planning prompt gave "approx 34.62 / 9.62"; the verified value is ~0.19 deg of longitude
 * (about 17 km) further west, i.e. ~0.8 minute of solar time. Only one source was available: see docs.
 */
public final class PrayerLocation {
    public static final double LAT = 34.574;
    public static final double LNG = 9.429;
    public static final ZoneId ZONE = ZoneId.of("Africa/Tunis");
    public static final int API_METHOD = 18; // Aladhan: Tunisia

    private PrayerLocation() {}
}

package com.ahmed2027.app.prayer;

/** Great-circle initial bearing to the Kaaba. Pure. */
public final class QiblaMath {
    public static final double KAABA_LAT = 21.4225, KAABA_LNG = 39.8262;

    private QiblaMath() {}

    /** Degrees clockwise from TRUE north, 0..360. */
    public static double bearing(double lat, double lng) {
        double p1 = Math.toRadians(lat), p2 = Math.toRadians(KAABA_LAT);
        double dl = Math.toRadians(KAABA_LNG - lng);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }

    /** Signed smallest rotation (-180..180) from the phone heading to the qibla. */
    public static double turnNeeded(double qibla, double heading) {
        double d = (qibla - heading) % 360.0;
        if (d > 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }
}

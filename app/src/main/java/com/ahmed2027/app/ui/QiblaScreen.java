package com.ahmed2027.app.ui;

import android.app.Activity;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ahmed2027.app.R;
import com.ahmed2027.app.prayer.PrayerLocation;
import com.ahmed2027.app.prayer.QiblaMath;

import java.util.Locale;

/** Qibla compass from the rotation-vector sensor; sensors run only while this screen is attached. */
public final class QiblaScreen extends ScreenBase implements SensorEventListener {
    private final SensorManager sm;
    private final Sensor rot;
    private final CompassView compass;
    private final TextView info, hint;
    private final float qibla;
    private final float declination;
    private int accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;

    public QiblaScreen(Activity a, Host host) {
        super(a, host, R.string.pr_qibla);
        sm = (SensorManager) a.getSystemService(Activity.SENSOR_SERVICE);
        rot = sm == null ? null : sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        qibla = (float) QiblaMath.bearing(PrayerLocation.LAT, PrayerLocation.LNG);
        declination = new GeomagneticField((float) PrayerLocation.LAT, (float) PrayerLocation.LNG, 0f, System.currentTimeMillis()).getDeclination();

        info = text(a.getString(R.string.qibla_bearing, String.format(Locale.US, "%.0f", qibla)), 18, R.color.text_primary);
        info.setGravity(Gravity.CENTER);
        col.addView(info, wrap(16));
        compass = new CompassView(a);
        compass.set(0f, qibla);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(a, 300));
        cl.topMargin = Ui.dp(a, 16);
        col.addView(compass, cl);
        hint = text(a.getString(rot == null ? R.string.qibla_no_sensor : R.string.qibla_hint), 14, R.color.text_secondary);
        hint.setGravity(Gravity.CENTER);
        col.addView(hint, wrap(16));

        sv.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {
                if (sm != null && rot != null) sm.registerListener(QiblaScreen.this, rot, SensorManager.SENSOR_DELAY_UI);
            }
            @Override public void onViewDetachedFromWindow(View v) { if (sm != null) sm.unregisterListener(QiblaScreen.this); }
        });
    }

    @Override public void onSensorChanged(SensorEvent e) {
        float[] r = new float[9], o = new float[3];
        SensorManager.getRotationMatrixFromVector(r, e.values);
        SensorManager.getOrientation(r, o);
        float magnetic = (float) Math.toDegrees(o[0]);
        float trueHeading = (magnetic + declination + 360f) % 360f; // phone held flat
        compass.set(trueHeading, qibla);
        boolean aligned = Math.abs(QiblaMath.turnNeeded(qibla, trueHeading)) < 5;
        hint.setText(accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW ? a.getString(R.string.qibla_calibrate)
                : a.getString(aligned ? R.string.qibla_aligned : R.string.qibla_hint));
    }

    @Override public void onAccuracyChanged(Sensor s, int acc) { accuracy = acc; }
}

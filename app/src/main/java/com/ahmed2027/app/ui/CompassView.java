package com.ahmed2027.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import com.ahmed2027.app.R;

/** Draws a dial that rotates with the phone and an arrow toward the qibla. Angles in degrees. */
public class CompassView extends View {
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG), arrow = new Paint(Paint.ANTI_ALIAS_FLAG),
            txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float heading, qibla;
    private boolean aligned;

    public CompassView(Context c) {
        super(c);
        ring.setStyle(Paint.Style.STROKE); ring.setStrokeWidth(Ui.dp(c, 3)); ring.setColor(c.getColor(R.color.divider));
        arrow.setStyle(Paint.Style.FILL);
        txt.setColor(c.getColor(R.color.text_secondary)); txt.setTextSize(Ui.dp(c, 18)); txt.setTextAlign(Paint.Align.CENTER);
    }

    public void set(float headingDeg, float qiblaDeg) {
        heading = headingDeg; qibla = qiblaDeg;
        double d = Math.abs(((qibla - heading) % 360 + 540) % 360 - 180);
        aligned = d < 5;
        invalidate();
    }

    @Override protected void onMeasure(int w, int h) {
        int s = Math.min(MeasureSpec.getSize(w), Ui.dp(getContext(), 300));
        setMeasuredDimension(s, s);
    }

    @Override protected void onDraw(Canvas cv) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) - Ui.dp(getContext(), 12);
        cv.drawCircle(cx, cy, r, ring);
        // north marker rotates opposite to the heading
        cv.save();
        cv.rotate(-heading, cx, cy);
        cv.drawText("N", cx, cy - r - Ui.dp(getContext(), 0) + txt.getTextSize() + Ui.dp(getContext(), 4), txt);
        cv.restore();
        // qibla arrow: angle from the top of the phone
        cv.save();
        cv.rotate(qibla - heading, cx, cy);
        arrow.setColor(getContext().getColor(aligned ? R.color.accent : R.color.text_primary));
        path.reset();
        path.moveTo(cx, cy - r * 0.8f);
        path.lineTo(cx - r * 0.12f, cy);
        path.lineTo(cx + r * 0.12f, cy);
        path.close();
        cv.drawPath(path, arrow);
        cv.restore();
        cv.drawCircle(cx, cy, Ui.dp(getContext(), 6), arrow);
    }
}

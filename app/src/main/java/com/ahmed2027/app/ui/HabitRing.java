package com.ahmed2027.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.ahmed2027.app.R;

/** Simple progress ring (done/total). total == 0 draws an empty track. */
public class HabitRing extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private int done, total;

    public HabitRing(Context c) {
        super(c);
        float w = Ui.dp(c, 8);
        track.setStyle(Paint.Style.STROKE); track.setStrokeWidth(w); track.setColor(c.getColor(R.color.divider));
        arc.setStyle(Paint.Style.STROKE); arc.setStrokeWidth(w); arc.setColor(c.getColor(R.color.accent));
        arc.setStrokeCap(Paint.Cap.ROUND);
    }

    public void set(int done, int total) { this.done = done; this.total = total; invalidate(); }

    @Override protected void onMeasure(int w, int h) {
        int s = Ui.dp(getContext(), 72);
        setMeasuredDimension(s, s);
    }

    @Override protected void onDraw(Canvas cv) {
        float pad = track.getStrokeWidth() / 2f;
        box.set(pad, pad, getWidth() - pad, getHeight() - pad);
        cv.drawArc(box, 0, 360, false, track);
        if (total > 0 && done > 0) cv.drawArc(box, -90, 360f * Math.min(done, total) / total, false, arc);
    }
}

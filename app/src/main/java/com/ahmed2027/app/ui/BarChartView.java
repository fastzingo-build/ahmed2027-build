package com.ahmed2027.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.ahmed2027.app.R;

/** Simple custom bar chart: one bar per day, no library. Today (last bar) uses the accent colour. */
public final class BarChartView extends View {
    private int[] values = new int[0];
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG), today = new Paint(Paint.ANTI_ALIAS_FLAG), base = new Paint();

    public BarChartView(Context c) {
        super(c);
        bar.setColor(c.getColor(R.color.text_secondary));
        today.setColor(c.getColor(R.color.accent));
        base.setColor(c.getColor(R.color.text_secondary));
        base.setAlpha(80);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void set(int[] v, CharSequence description) { values = v; setContentDescription(description); invalidate(); }

    @Override protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), Ui.dp(getContext(), 72)); }

    @Override protected void onDraw(Canvas c) {
        int n = values.length;
        if (n == 0) return;
        int max = 1;
        for (int x : values) max = Math.max(max, x);
        float w = getWidth(), h = getHeight() - 2, slot = w / n, gap = Math.min(slot * 0.25f, 6f);
        c.drawRect(0, h, w, h + 2, base);
        for (int i = 0; i < n; i++) {
            float bh = values[i] == 0 ? 2f : Math.max(4f, h * values[i] / max);
            RectF r = new RectF(i * slot + gap, h - bh, (i + 1) * slot - gap, h);
            c.drawRoundRect(r, 3f, 3f, i == n - 1 ? today : bar);
        }
    }
}

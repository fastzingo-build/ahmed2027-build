package com.ahmed2027.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ahmed2027.app.R;

/** 5-item bottom bar: Home, Apps, Grow, Work, More. */
public class BottomNav extends LinearLayout {
    public interface Listener { void onSelected(int index); }

    private static final int[] LABELS = {
            R.string.tab_home, R.string.tab_apps, R.string.tab_grow, R.string.tab_work, R.string.tab_more};

    private final TextView[] items = new TextView[LABELS.length];
    private Listener listener;
    private int selected = -1;

    public BottomNav(Context c) {
        super(c);
        setOrientation(HORIZONTAL);
        setBackgroundColor(c.getColor(R.color.card));
        setContentDescription(c.getString(R.string.nav_label));
        for (int i = 0; i < LABELS.length; i++) {
            final int idx = i;
            TextView t = new TextView(c);
            t.setText(LABELS[i]);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(13);
            t.setClickable(true);
            t.setFocusable(true);
            t.setMinHeight(Ui.dp(c, 64));
            t.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) { select(idx, true); }
            });
            items[i] = t;
            addView(t, new LayoutParams(0, Ui.dp(c, 64), 1f));
        }
    }

    public void setListener(Listener l) { this.listener = l; }

    public int getSelected() { return selected; }

    /** notify=false for restoring state without firing the listener. */
    public void select(int index, boolean notify) {
        if (index < 0 || index >= items.length) return;
        boolean changed = index != selected;
        selected = index;
        Context c = getContext();
        for (int i = 0; i < items.length; i++) {
            boolean on = i == index;
            items[i].setTextColor(c.getColor(on ? R.color.accent : R.color.text_secondary));
            items[i].setTypeface(on ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            items[i].setSelected(on);
        }
        if (notify && changed && listener != null) listener.onSelected(index);
    }
}

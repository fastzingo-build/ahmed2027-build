package com.ahmed2027.app.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ahmed2027.app.R;

/** Base for centered full-area states (empty / error / loading). */
abstract class StateView extends LinearLayout {
    StateView(Context c) {
        super(c);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        int p = Ui.dp(c, 24);
        setPadding(p, p, p, p);
    }

    TextView addTitle(CharSequence t) {
        TextView v = new TextView(getContext());
        v.setText(t);
        v.setTextColor(getContext().getColor(R.color.text_primary));
        v.setTextSize(18);
        v.setGravity(Gravity.CENTER);
        v.setAccessibilityHeading(true);
        addView(v);
        return v;
    }

    TextView addBody(CharSequence t) {
        TextView v = new TextView(getContext());
        v.setText(t);
        v.setTextColor(getContext().getColor(R.color.text_secondary));
        v.setTextSize(15);
        v.setLineSpacing(0, 1.25f);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, Ui.dp(getContext(), 8), 0, 0);
        addView(v);
        return v;
    }

    void addAction(CharSequence label, OnClickListener l) {
        PrimaryButton b = new PrimaryButton(getContext(), label, true);
        b.setOnClickListener(l);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(getContext(), 16);
        addView(b, lp);
    }

}

package com.ahmed2027.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;

import com.ahmed2027.app.R;

public class SectionTitle extends TextView {
    public SectionTitle(Context c, CharSequence text) {
        super(c);
        setText(text);
        setTextColor(c.getColor(R.color.text_secondary));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        setTypeface(Typeface.DEFAULT_BOLD);
        setPadding(0, Ui.dp(c, 16), 0, Ui.dp(c, 8));
        setAccessibilityHeading(true);
    }
}

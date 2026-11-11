package com.ahmed2027.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.Button;

import com.ahmed2027.app.R;

/** Accent button, at least 48dp tall. Pass filled=false for an outlined-looking secondary. */
public class PrimaryButton extends Button {
    public PrimaryButton(Context c, CharSequence text, boolean filled) {
        super(c);
        setText(text);
        setAllCaps(false);
        setStateListAnimator(null);
        setTypeface(Typeface.DEFAULT_BOLD);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        setMinHeight(Ui.dp(c, 48));
        setMinimumHeight(Ui.dp(c, 48));
        int pad = Ui.dp(c, 16);
        setPadding(pad, 0, pad, 0);
        if (filled) {
            setBackground(Ui.rounded(c, c.getColor(R.color.accent), 16));
            setTextColor(c.getColor(R.color.on_accent));
        } else {
            setBackground(Ui.rounded(c, c.getColor(R.color.divider), 16));
            setTextColor(c.getColor(R.color.text_primary));
        }
    }
}

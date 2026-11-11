package com.ahmed2027.app.ui;

import android.content.Context;
import android.widget.LinearLayout;

import com.ahmed2027.app.R;

/** Rounded 20dp surface container. */
public class Card extends LinearLayout {
    public Card(Context c) {
        super(c);
        setOrientation(VERTICAL);
        setBackground(Ui.rounded(c, c.getColor(R.color.card), 20));
        int p = Ui.dp(c, 16);
        setPadding(p, p, p, p);
        setElevation(Ui.dp(c, 2));
    }
}

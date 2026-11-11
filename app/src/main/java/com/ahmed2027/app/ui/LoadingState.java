package com.ahmed2027.app.ui;

import android.content.Context;
import android.widget.ProgressBar;

import com.ahmed2027.app.R;

public class LoadingState extends StateView {
    public LoadingState(Context c) {
        super(c);
        ProgressBar p = new ProgressBar(c);
        p.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(c.getColor(R.color.accent)));
        addView(p);
        addBody(c.getString(R.string.state_loading));
    }
}

package com.ahmed2027.app.ui;

import android.content.Context;

import com.ahmed2027.app.R;

/** Error with a retry action that the caller wires to real work. */
public class ErrorState extends StateView {
    public ErrorState(Context c, CharSequence detail, OnClickListener retry) {
        super(c);
        addTitle(c.getString(R.string.state_error_title));
        if (detail != null) addBody(detail);
        if (retry != null) addAction(c.getString(R.string.state_error_retry), retry);
    }
}

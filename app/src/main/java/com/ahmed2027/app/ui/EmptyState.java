package com.ahmed2027.app.ui;

import android.content.Context;

/** Nothing here yet. Optional action only if it really does something. */
public class EmptyState extends StateView {
    public EmptyState(Context c, CharSequence title, CharSequence body) {
        super(c);
        addTitle(title);
        addBody(body);
    }
}

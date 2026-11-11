package com.ahmed2027.app.ui;

import android.view.View;

/** Runs a task every periodMs only while the view is attached to a window (no leaked timers). */
public final class Ticker {
    private Ticker() {}

    public static void attach(final View v, final long periodMs, final Runnable task) {
        final Runnable loop = new Runnable() {
            @Override public void run() {
                if (!v.isAttachedToWindow()) return;
                task.run();
                v.postDelayed(this, periodMs);
            }
        };
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View x) { x.removeCallbacks(loop); x.post(loop); }
            @Override public void onViewDetachedFromWindow(View x) { x.removeCallbacks(loop); }
        });
    }
}

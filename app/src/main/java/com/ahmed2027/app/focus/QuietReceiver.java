package com.ahmed2027.app.focus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarm tick and boot/time-change events: re-evaluate quiet periods. */
public final class QuietReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) { QuietAlarms.apply(c.getApplicationContext()); }
}

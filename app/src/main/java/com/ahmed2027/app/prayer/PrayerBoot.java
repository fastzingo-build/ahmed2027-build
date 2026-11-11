package com.ahmed2027.app.prayer;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** App-start hook: refresh the cache in the background when stale, then (re)schedule the alarm. */
public final class PrayerBoot {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private PrayerBoot() {}

    public static void onAppStart(final Context c) {
        final Context app = c.getApplicationContext();
        PrayerNotifier.ensureChannels(app);
        PrayerAlarms.scheduleNext(app); // immediately, with whatever data exists
        IO.execute(new Runnable() { @Override public void run() {
            PrayerRepo.refresh(app, false);   // never blocks the UI; failure just keeps the cache/estimate
            PrayerAlarms.scheduleNext(app);   // times may have changed
        } });
    }
}

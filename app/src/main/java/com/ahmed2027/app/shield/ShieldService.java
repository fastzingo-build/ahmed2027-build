package com.ahmed2027.app.shield;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.friendkey.FriendKeyGate;
import com.ahmed2027.app.friendkey.UnlockManager;
import com.ahmed2027.app.launcher.AppClass;
import com.ahmed2027.app.launcher.AppClassStore;
import com.ahmed2027.app.prayer.PrayerLocation;
import com.ahmed2027.app.prayer.PrayerProvider;
import com.ahmed2027.app.prayer.PrayerSettings;
import com.ahmed2027.app.social.SocialSessionManager;
import com.ahmed2027.app.social.SqlSessionStore;

import java.time.ZonedDateTime;
import java.util.Set;

/**
 * Event-driven foreground detection. Reads ONLY the package name of window-change events.
 * canRetrieveWindowContent=false (see res/xml/shield_service.xml): no screen text, no
 * passwords, no notification content is ever available to this service.
 */
public class ShieldService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Set<String> never;
    private String lastPkg;
    private final Runnable expiryCheck = new Runnable() {
        @Override public void run() { if (lastPkg != null) evaluate(lastPkg); }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        never = NeverBlock.resolve(this);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cs = e.getPackageName();
        if (cs == null) return;
        String pkg = cs.toString();
        if (pkg.equals(getPackageName())) { lastPkg = pkg; return; } // our own screens: remember, never act
        if (!trackable(pkg)) return;                                  // keyboards, system UI, ...
        lastPkg = pkg;
        evaluate(pkg);
    }

    private boolean trackable(String pkg) {
        if (pkg.equals("com.android.systemui") || pkg.contains(".inputmethod")) return false;
        return getPackageManager().getLaunchIntentForPackage(pkg) != null;
    }

    private void evaluate(String pkg) {
        try {
            if (never == null) never = NeverBlock.resolve(this);
            AppClassStore.Entry en = AppClassStore.all(this).get(pkg);
            String cls = en == null ? AppClass.ALLOWED : en.cls;
            String key = SocialSessionManager.quotaKey(pkg);
            SocialSessionManager mgr = SqlSessionStore.manager(this);
            long wall = Clock.wall(), el = Clock.elapsed();
            SocialSessionManager.State st = key == null ? null : mgr.state(key, wall, el);
            long unlockLeft = FriendKeyGate.remaining(this, UnlockManager.APP, pkg);
            long windowEnd = PrayerProvider.focusWindowEnd(this, ZonedDateTime.now(PrayerLocation.ZONE));
            ShieldPolicy.Decision d = ShieldPolicy.decide(pkg, getPackageName(), never, ShieldStatus.isOn(this),
                    cls, LimitedPass.valid(this, pkg), unlockLeft > 0, windowEnd > 0, st);
            handler.removeCallbacks(expiryCheck);
            if (d == ShieldPolicy.Decision.ALLOW) {
                // One wake-up at the earliest moment the answer could change (the user may be scrolling
                // without any window change): social session end, unlock end, or the next adhan.
                long wake = Long.MAX_VALUE;
                if (st == SocialSessionManager.State.ACTIVE) wake = Math.min(wake, mgr.remainingMillis(key, wall, el));
                if (unlockLeft > 0) wake = Math.min(wake, unlockLeft);
                boolean prayerSensitive = AppClass.LIMITED.equals(cls) || key != null;
                if (prayerSensitive && PrayerSettings.focusWindowOn(this)) {
                    wake = Math.min(wake, Math.max(1000, PrayerProvider.next(this).timeWall - wall));
                }
                if (wake != Long.MAX_VALUE) handler.postDelayed(expiryCheck, wake + 500);
                return;
            }
            show(d, pkg);
        } catch (RuntimeException ex) {
            // Never crash, never lock the user out: on any error simply do nothing.
        }
    }

    private void show(ShieldPolicy.Decision d, String pkg) {
        Intent i = new Intent(this, BlockedActivity.class)
                .putExtra(BlockedActivity.EXTRA_MODE, d.name())
                .putExtra(BlockedActivity.EXTRA_PKG, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            startActivity(i);
        } catch (RuntimeException ex) {
            performGlobalAction(GLOBAL_ACTION_HOME); // fallback: at least leave the app
        }
    }

    @Override public void onInterrupt() { handler.removeCallbacks(expiryCheck); }

    @Override public void onDestroy() {
        handler.removeCallbacks(expiryCheck);
        super.onDestroy();
    }
}

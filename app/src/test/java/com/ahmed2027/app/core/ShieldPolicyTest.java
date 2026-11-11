package com.ahmed2027.app.core;

import static com.ahmed2027.app.shield.ShieldPolicy.Decision.*;
import static org.junit.Assert.*;

import com.ahmed2027.app.launcher.AppClass;
import com.ahmed2027.app.shield.ShieldPolicy;
import com.ahmed2027.app.social.SocialSessionManager.State;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ShieldPolicyTest {
    private static final String OWN = "com.ahmed2027.app";
    private static final Set<String> NEVER = new HashSet<>(Arrays.asList("com.android.dialer", "com.android.settings"));

    private static ShieldPolicy.Decision d(String pkg, boolean on, String cls, boolean pass, State social) {
        return ShieldPolicy.decide(pkg, OWN, NEVER, on, cls, pass, false, false, social);
    }

    @Test public void friendKeyUnlockAllowsBlockedLimitedAndUsedSocial() {
        assertEquals(ALLOW, ShieldPolicy.decide("com.x", OWN, NEVER, true, AppClass.BLOCKED, false, true, false, null));
        assertEquals(ALLOW, ShieldPolicy.decide("com.x", OWN, NEVER, true, AppClass.LIMITED, false, true, false, null));
        assertEquals(ALLOW, ShieldPolicy.decide("com.instagram.android", OWN, NEVER, true, AppClass.ALLOWED, false, true, false, State.EXPIRED));
    }

    @Test public void neverBlockListWinsOverBlockedClass() {
        assertEquals(ALLOW, d("com.android.dialer", true, AppClass.BLOCKED, false, null));
        assertEquals(ALLOW, d("com.android.settings", true, AppClass.BLOCKED, false, null));
    }
    @Test public void ownAppIsNeverBlocked() { assertEquals(ALLOW, d(OWN, true, AppClass.BLOCKED, false, null)); }
    @Test public void shieldOffAllowsEverything() {
        assertEquals(ALLOW, d("com.x", false, AppClass.BLOCKED, false, null));
        assertEquals(ALLOW, d("com.instagram.android", false, AppClass.ALLOWED, false, State.EXPIRED));
    }
    @Test public void blockedAppShowsBlockedScreen() { assertEquals(BLOCKED_SCREEN, d("com.x", true, AppClass.BLOCKED, false, null)); }
    @Test public void allowedAndHiddenAppsAreAllowed() {
        assertEquals(ALLOW, d("com.x", true, AppClass.ALLOWED, false, null));
        assertEquals(ALLOW, d("com.x", true, AppClass.HIDDEN, false, null)); // hidden != blocked
    }
    @Test public void limitedNeedsPause_unlessPassValid() {
        assertEquals(LIMITED_PAUSE, d("com.x", true, AppClass.LIMITED, false, null));
        assertEquals(ALLOW, d("com.x", true, AppClass.LIMITED, true, null));
    }
    @Test public void socialStates() {
        assertEquals(ALLOW, d("com.instagram.android", true, AppClass.ALLOWED, false, State.ACTIVE));
        assertEquals(SOCIAL_START, d("com.instagram.android", true, AppClass.ALLOWED, false, State.ELIGIBLE));
        assertEquals(SOCIAL_USED, d("com.instagram.android", true, AppClass.ALLOWED, false, State.EXPIRED));
    }
    @Test public void blockedClassBeatsSocialSession() {
        assertEquals(BLOCKED_SCREEN, d("com.instagram.android", true, AppClass.BLOCKED, false, State.ACTIVE));
    }
    @Test public void limitedPassDoesNotBypassSocialQuota() {
        assertEquals(SOCIAL_USED, d("com.instagram.android", true, AppClass.LIMITED, true, State.EXPIRED));
    }
    private static ShieldPolicy.Decision p(String pkg, boolean on, String cls, boolean unlocked, boolean window, State social) {
        return ShieldPolicy.decide(pkg, OWN, NEVER, on, cls, false, unlocked, window, social);
    }
    @Test public void prayerWindowPausesLimitedAndSocialOnly() {
        assertEquals(PRAYER_PAUSE, p("com.x", true, AppClass.LIMITED, false, true, null));
        assertEquals(PRAYER_PAUSE, p("com.instagram.android", true, AppClass.ALLOWED, false, true, State.ACTIVE));
        assertEquals(ALLOW, p("com.x", true, AppClass.ALLOWED, false, true, null));      // ordinary apps untouched
        assertEquals(ALLOW, p("com.x", true, AppClass.HIDDEN, false, true, null));
        assertEquals(LIMITED_PAUSE, p("com.x", true, AppClass.LIMITED, false, false, null)); // window closed: normal rule
    }
    @Test public void prayerWindowNeverTouchesEmergencyOrOwnAppAndFriendKeyBeatsIt() {
        assertEquals(ALLOW, p("com.android.dialer", true, AppClass.LIMITED, false, true, null));
        assertEquals(ALLOW, p(OWN, true, AppClass.LIMITED, false, true, null));
        assertEquals(ALLOW, p("com.x", true, AppClass.LIMITED, true, true, null));
        assertEquals(ALLOW, p("com.x", false, AppClass.LIMITED, false, true, null)); // shield off
    }
    @Test public void nullPackageIsAllowed() { assertEquals(ALLOW, d(null, true, AppClass.BLOCKED, false, null)); }
}

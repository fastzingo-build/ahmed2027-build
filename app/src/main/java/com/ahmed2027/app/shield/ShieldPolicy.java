package com.ahmed2027.app.shield;

import com.ahmed2027.app.launcher.AppClass;
import com.ahmed2027.app.social.SocialSessionManager;

import java.util.Set;

/** Pure decision rules for "what should happen when this app comes to the foreground". */
public final class ShieldPolicy {
    public enum Decision { ALLOW, BLOCKED_SCREEN, LIMITED_PAUSE, SOCIAL_START, SOCIAL_USED, PRAYER_PAUSE }

    private ShieldPolicy() {}

    /**
     * @param socialState state of the app's quota, or null when it is not a social app
     * Order matters: shield off / never-block list always win, then a Friend Key unlock, then BLOCKED, then the prayer window, then social, then LIMITED.
     */
    public static Decision decide(String pkg, String ownPkg, Set<String> neverBlock, boolean shieldOn,
                                  String appClass, boolean limitedPassValid, boolean appUnlocked, boolean prayerWindow,
                                  SocialSessionManager.State socialState) {
        if (!shieldOn || pkg == null) return Decision.ALLOW;
        if (pkg.equals(ownPkg) || neverBlock.contains(pkg)) return Decision.ALLOW;
        if (appUnlocked) return Decision.ALLOW; // Friend Key unlock (15 min), auto re-locks when it expires
        String cls = AppClass.normalize(appClass);
        if (AppClass.BLOCKED.equals(cls)) return Decision.BLOCKED_SCREEN;
        // Optional prayer-focus window (user toggle, off by default): LIMITED and social apps rest for a few minutes.
        if (prayerWindow && (AppClass.LIMITED.equals(cls) || socialState != null)) return Decision.PRAYER_PAUSE;
        if (socialState != null) {
            switch (socialState) {
                case ACTIVE: return Decision.ALLOW;
                case ELIGIBLE: return Decision.SOCIAL_START;
                default: return Decision.SOCIAL_USED;
            }
        }
        if (AppClass.LIMITED.equals(cls)) return limitedPassValid ? Decision.ALLOW : Decision.LIMITED_PAUSE;
        return Decision.ALLOW;
    }
}

package com.ahmed2027.app.shield;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.LocaleManager;
import com.ahmed2027.app.friendkey.FriendKeyGate;
import com.ahmed2027.app.friendkey.UnlockManager;
import com.ahmed2027.app.social.SocialSessionManager;
import com.ahmed2027.app.social.SqlSessionStore;
import com.ahmed2027.app.ui.Card;
import com.ahmed2027.app.ui.PrimaryButton;
import com.ahmed2027.app.ui.Ui;

/**
 * AHMED's own, honest screen: it says plainly what is happening and why. It is a normal activity
 * (no overlay, no fake system look). Options that do not exist yet (Friend Key, alternative
 * activities) are NOT shown, so there are no fake buttons.
 */
public class BlockedActivity extends Activity {
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_PKG = "pkg";
    private static final int PAUSE_SECONDS = 10;

    private CountDownTimer timer;
    private ShieldPolicy.Decision mode;
    private String pkg;
    private String label;

    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(LocaleManager.wrap(base)); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        pkg = getIntent().getStringExtra(EXTRA_PKG);
        try { mode = ShieldPolicy.Decision.valueOf(getIntent().getStringExtra(EXTRA_MODE)); }
        catch (RuntimeException e) { goHome(); return; }
        label = pkg;
        try { label = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)).toString(); }
        catch (PackageManager.NameNotFoundException | RuntimeException ignored) { /* keep package name */ }
        render();
    }

    private void render() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(getColor(R.color.bg));
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(this, 24);
        root.setPadding(p, p, p, p);
        sv.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int titleRes, bodyRes;
        switch (mode) {
            case BLOCKED_SCREEN: titleRes = R.string.blk_blocked_title; bodyRes = R.string.blk_blocked_body; break;
            case LIMITED_PAUSE: titleRes = R.string.blk_limited_title; bodyRes = R.string.blk_limited_body; break;
            case PRAYER_PAUSE: titleRes = R.string.blk_prayer_title; bodyRes = R.string.blk_prayer_body; break;
            case SOCIAL_START: titleRes = R.string.blk_social_title; bodyRes = R.string.blk_social_body; break;
            default: titleRes = R.string.blk_used_title; bodyRes = R.string.blk_used_body; break;
        }
        Card card = new Card(this);
        TextView t = new TextView(this);
        t.setText(getString(titleRes, label));
        t.setTextColor(getColor(R.color.text_primary));
        t.setTextSize(24);
        t.setAccessibilityHeading(true);
        card.addView(t);
        TextView body = new TextView(this);
        body.setText(getString(bodyRes, label));
        body.setTextColor(getColor(R.color.text_secondary));
        body.setTextSize(16);
        body.setLineSpacing(0, 1.3f);
        body.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(body);
        root.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final PrimaryButton proceed;
        if (mode == ShieldPolicy.Decision.LIMITED_PAUSE || mode == ShieldPolicy.Decision.SOCIAL_START) {
            final boolean social = mode == ShieldPolicy.Decision.SOCIAL_START;
            proceed = new PrimaryButton(this, "", false);
            proceed.setEnabled(false);
            proceed.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { if (social) startSocial(); else continueLimited(); }
            });
            root.addView(proceed, gap());
            final int actionRes = social ? R.string.blk_start_session : R.string.blk_continue;
            timer = new CountDownTimer(PAUSE_SECONDS * 1000L, 1000) {
                @Override public void onTick(long ms) {
                    proceed.setText(getString(R.string.blk_wait, (ms + 999) / 1000));
                }
                @Override public void onFinish() {
                    proceed.setText(actionRes);
                    proceed.setEnabled(true);
                    proceed.setBackground(Ui.rounded(BlockedActivity.this, getColor(R.color.accent), 16));
                    proceed.setTextColor(getColor(R.color.on_accent));
                }
            }.start();
        }

        if (mode != ShieldPolicy.Decision.SOCIAL_START) {
            PrimaryButton ask = new PrimaryButton(this, getString(R.string.blk_ask_friend), false);
            ask.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivityForResult(FriendKeyGate.intent(BlockedActivity.this, UnlockManager.APP, pkg),
                            FriendKeyGate.REQ_KEY);
                }
            });
            root.addView(ask, gap());
        }

        PrimaryButton home = new PrimaryButton(this, getString(R.string.blk_go_home), true);
        home.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { goHome(); } });
        root.addView(home, gap());
        setContentView(sv);
    }

    private LinearLayout.LayoutParams gap() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 16);
        return lp;
    }

    private void continueLimited() {
        LimitedPass.grant(this, pkg);
        openTarget();
    }

    private void startSocial() {
        String key = SocialSessionManager.quotaKey(pkg);
        if (key == null) { goHome(); return; }
        SocialSessionManager.Session s = SqlSessionStore.manager(this).start(key, Clock.wall(), Clock.elapsed());
        if (s == null) { // somebody already started today's session; show the honest "used" screen
            mode = ShieldPolicy.Decision.SOCIAL_USED;
            render();
            return;
        }
        openTarget();
    }

    private void openTarget() {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) startActivity(i);
        finish();
    }

    private void goHome() {
        Intent h = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(h);
        finish();
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == FriendKeyGate.REQ_KEY && res == RESULT_OK) finish(); // FriendKeyActivity already opened the app
    }

    @Override public void onBackPressed() { goHome(); }

    @Override protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}

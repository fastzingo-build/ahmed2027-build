package com.ahmed2027.app.friendkey;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.LocaleManager;
import com.ahmed2027.app.ui.Card;
import com.ahmed2027.app.ui.PrimaryButton;
import com.ahmed2027.app.ui.Ui;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Friend Key screen. Never gated by anything (it is the way OUT of restrictions), does not
 * touch Android itself, and a wrong key only delays further attempts here.
 */
public class FriendKeyActivity extends Activity {
    public static final String EXTRA_SCOPE = "scope";
    public static final String EXTRA_TARGET = "target";

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private FriendKeyStore store;
    private AttemptLimiter limiter;
    private String scope, target;
    private CountDownTimer ticker;
    private boolean verifying;

    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(LocaleManager.wrap(base)); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        // The key is shown on screen: keep it out of screenshots and the recents thumbnail.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        store = new FriendKeyStore(this);
        limiter = new AttemptLimiter(store);
        scope = getIntent().getStringExtra(EXTRA_SCOPE);
        if (scope == null) scope = UnlockManager.SETTINGS;
        target = getIntent().getStringExtra(EXTRA_TARGET);
        render();
    }

    @Override protected void onResume() { super.onResume(); render(); }

    @Override protected void onPause() {
        if (ticker != null) { ticker.cancel(); ticker = null; }
        super.onPause();
    }

    private boolean coolingPending() {
        if (!store.coolingEnabled()) return false;
        return !CoolingOff.ready(store.coolStarted(), store.coolWall(), store.coolElapsed(), Clock.wall(), Clock.elapsed());
    }

    private void render() {
        if (ticker != null) { ticker.cancel(); ticker = null; }
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(getColor(R.color.bg));
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(this, 24);
        root.setPadding(p, p, p, p);
        sv.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Card card = new Card(this);
        card.addView(text(R.string.fk_title, 24, R.color.text_primary, true));
        TextView ex = text(scopeExplain(), 15, R.color.text_secondary, false);
        ex.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(ex);
        root.addView(card, wrap());

        if (coolingPending()) buildCooling(root); else buildKeyEntry(root);

        PrimaryButton cancel = new PrimaryButton(this, getString(R.string.action_cancel), false);
        cancel.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });
        root.addView(cancel, gap());
        setContentView(sv);
    }

    private int scopeExplain() {
        if (UnlockManager.APP.equals(scope)) return R.string.fk_explain_app;
        if (UnlockManager.CLASSIFY.equals(scope)) return R.string.fk_explain_classify;
        return R.string.fk_explain_settings;
    }

    // ---- 5-minute cooling-off
    private void buildCooling(LinearLayout root) {
        final TextView status = text(R.string.fk_cool_intro, 16, R.color.text_primary, false);
        status.setPadding(0, Ui.dp(this, 16), 0, 0);
        root.addView(status, wrap());
        if (!store.coolStarted() || CoolingOff.remainingMillis(store.coolWall(), store.coolElapsed(), Clock.wall(), Clock.elapsed()) == 0) {
            // not started, or a previous wait went stale: offer to start
            PrimaryButton start = new PrimaryButton(this, getString(R.string.fk_cool_start), true);
            start.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { store.startCooling(Clock.wall(), Clock.elapsed()); render(); }
            });
            root.addView(start, gap());
            return;
        }
        long rem = CoolingOff.remainingMillis(store.coolWall(), store.coolElapsed(), Clock.wall(), Clock.elapsed());
        status.setText(getString(R.string.fk_cool_waiting, fmt(rem)));
        ticker = new CountDownTimer(rem, 1000) {
            @Override public void onTick(long ms) {
                long r = CoolingOff.remainingMillis(store.coolWall(), store.coolElapsed(), Clock.wall(), Clock.elapsed());
                status.setText(getString(R.string.fk_cool_waiting, fmt(r)));
            }
            @Override public void onFinish() { render(); }
        }.start();
    }

    // ---- key entry
    private void buildKeyEntry(LinearLayout root) {
        final EditText field = new EditText(this);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setHint(R.string.fk_hint);
        field.setHintTextColor(getColor(R.color.text_secondary));
        field.setTextColor(getColor(R.color.text_primary));
        field.setSingleLine(true);
        field.setBackground(Ui.rounded(this, getColor(R.color.card), 16));
        field.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        LinearLayout.LayoutParams fl = gap();
        fl.height = Ui.dp(this, 56);
        root.addView(field, fl);

        CheckBox show = new CheckBox(this);
        show.setText(R.string.fk_show);
        show.setTextColor(getColor(R.color.text_secondary));
        show.setMinHeight(Ui.dp(this, 48));
        show.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) {
                int sel = field.getSelectionEnd();
                field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                        | (on ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
                field.setSelection(Math.max(0, sel));
            }
        });
        root.addView(show, wrap());

        final TextView msg = text(R.string.fk_status_ready, 15, R.color.text_secondary, false);
        root.addView(msg, wrap());

        final PrimaryButton verify = new PrimaryButton(this, getString(R.string.fk_verify), true);
        root.addView(verify, gap());
        verify.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { attempt(field, msg, verify); }
        });
        updateLockState(msg, verify);
    }

    private void updateLockState(final TextView msg, final PrimaryButton verify) {
        long rem = limiter.remainingMillis(Clock.wall(), Clock.elapsed());
        if (rem <= 0) { verify.setEnabled(true); return; }
        verify.setEnabled(false);
        if (ticker != null) ticker.cancel();
        ticker = new CountDownTimer(rem, 1000) {
            @Override public void onTick(long ms) {
                long r = limiter.remainingMillis(Clock.wall(), Clock.elapsed());
                msg.setTextColor(getColor(R.color.warning));
                msg.setText(getString(R.string.fk_status_locked, fmt(r)));
            }
            @Override public void onFinish() {
                msg.setTextColor(getColor(R.color.text_secondary));
                msg.setText(R.string.fk_status_ready);
                verify.setEnabled(true);
            }
        }.start();
    }

    private void attempt(final EditText field, final TextView msg, final PrimaryButton verify) {
        if (verifying) return;
        if (!limiter.canAttempt(Clock.wall(), Clock.elapsed())) { updateLockState(msg, verify); return; }
        int n = field.getText().length();
        if (n == 0) { msg.setText(R.string.fk_status_empty); return; }
        final char[] key = new char[n];
        field.getText().getChars(0, n, key, 0);
        field.setText(""); // clear the visible copy right away
        verifying = true;
        verify.setEnabled(false);
        msg.setTextColor(getColor(R.color.text_secondary));
        msg.setText(R.string.fk_status_checking);
        IO.execute(new Runnable() { @Override public void run() {
            final boolean ok = FriendKey.verify(key); // zeroes `key`
            runOnUiThread(new Runnable() { @Override public void run() {
                verifying = false;
                if (isFinishing() || isDestroyed()) return;
                if (ok) onSuccess(); else onWrong(msg, verify);
            } });
        } });
    }

    private void onSuccess() {
        limiter.recordSuccess();
        store.clearCooling();
        FriendKeyGate.unlocks(this).grant(scope, target, Clock.wall(), Clock.elapsed());
        FriendKeyGate.logUnlock(this, scope, target);
        setResult(RESULT_OK);
        if (UnlockManager.APP.equals(scope) && target != null) {
            Intent i = getPackageManager().getLaunchIntentForPackage(target);
            if (i != null) startActivity(i);
        }
        finish();
    }

    private void onWrong(TextView msg, PrimaryButton verify) {
        long delay = limiter.recordFailure(Clock.wall(), Clock.elapsed());
        FriendKeyGate.logWrong(this, limiter.failures());
        msg.setTextColor(getColor(R.color.warning));
        msg.setText(R.string.fk_status_wrong);
        verify.setEnabled(true);
        if (delay > 0) updateLockState(msg, verify);
    }

    private static String fmt(long ms) {
        long s = (ms + 999) / 1000;
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    private TextView text(int res, int sp, int color, boolean heading) {
        TextView t = new TextView(this);
        t.setText(res);
        t.setTextSize(sp);
        t.setTextColor(getColor(color));
        t.setLineSpacing(0, 1.25f);
        if (heading) t.setAccessibilityHeading(true);
        return t;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams gap() {
        LinearLayout.LayoutParams lp = wrap();
        lp.topMargin = Ui.dp(this, 16);
        return lp;
    }
}

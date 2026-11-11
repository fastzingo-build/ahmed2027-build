package com.ahmed2027.app.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.prayer.PrayerAlarms;
import com.ahmed2027.app.prayer.*;

/** Reminders (explanation first), sound, manual offsets (+-10 min), optional prayer-focus window. */
public final class PrayerSettingsScreen extends ScreenBase {

    public PrayerSettingsScreen(Activity a, Host host) {
        super(a, host, R.string.pr_settings);
        build();
    }

    private void changed() { PrayerAlarms.scheduleNext(a.getApplicationContext()); build(); }

    private void build() {
        clearBody();

        // ---- reminders
        col.addView(new SectionTitle(a, a.getString(R.string.pr_reminders)));
        Card rem = new Card(a);
        rem.addView(text(a.getString(R.string.pr_reminders_explain), 14, R.color.text_secondary));
        final boolean on = PrayerSettings.remindersOn(a);
        rem.addView(button(on ? R.string.pr_reminders_off : R.string.pr_reminders_on, !on, new View.OnClickListener() {
            @Override public void onClick(View v) {
                PrayerSettings.setRemindersOn(a, !PrayerSettings.remindersOn(a));
                PrayerNotifier.ensureChannels(a);
                changed();
            } }), wrap(12));
        if (on) {
            boolean exact = PrayerAlarms.canExact(a);
            TextView ex = text(a.getString(exact ? R.string.pr_exact_ok : R.string.pr_exact_missing), 14,
                    exact ? R.color.accent : R.color.warning);
            ex.setPadding(0, Ui.dp(a, 12), 0, 0);
            rem.addView(ex);
            if (!exact) {
                rem.addView(button(R.string.pr_exact_grant, true, new View.OnClickListener() {
                    @Override public void onClick(View v) { openExactAlarmSettings(); } }), wrap(8));
            }
            NotificationManager nm = (NotificationManager) a.getSystemService(Activity.NOTIFICATION_SERVICE);
            if (nm != null && !nm.areNotificationsEnabled()) {
                TextView nt = text(a.getString(R.string.pr_notif_blocked), 14, R.color.warning);
                nt.setPadding(0, Ui.dp(a, 12), 0, 0);
                rem.addView(nt);
                rem.addView(button(R.string.pr_notif_open, true, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        try { a.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName())); }
                        catch (ActivityNotFoundException e) { Toast.makeText(a, R.string.pr_open_failed, Toast.LENGTH_LONG).show(); }
                    } }), wrap(8));
            }
            // MIUI hint: the app may be killed in the background
            TextView miui = text(a.getString(R.string.pr_miui_hint), 13, R.color.text_secondary);
            miui.setPadding(0, Ui.dp(a, 12), 0, 0);
            rem.addView(miui);
        }
        col.addView(rem, wrap(0));

        // ---- sound
        col.addView(new SectionTitle(a, a.getString(R.string.pr_sound)));
        Card snd = new Card(a);
        String cur = PrayerSettings.sound(a);
        snd.addView(soundButton(R.string.pr_sound_default, PrayerSettings.SOUND_DEFAULT, cur), wrap(0));
        snd.addView(soundButton(R.string.pr_sound_alarm, PrayerSettings.SOUND_ALARM, cur), wrap(8));
        snd.addView(soundButton(R.string.pr_sound_silent, PrayerSettings.SOUND_SILENT, cur), wrap(8));
        col.addView(snd, wrap(0));

        // ---- offsets
        col.addView(new SectionTitle(a, a.getString(R.string.pr_offsets)));
        Card off = new Card(a);
        off.addView(text(a.getString(R.string.pr_offsets_explain), 14, R.color.text_secondary));
        for (Prayer p : Prayer.values()) off.addView(offsetRow(p), wrap(8));
        col.addView(off, wrap(0));

        // ---- focus window
        col.addView(new SectionTitle(a, a.getString(R.string.pr_focus)));
        Card foc = new Card(a);
        foc.addView(text(a.getString(R.string.pr_focus_explain), 14, R.color.text_secondary));
        final boolean f = PrayerSettings.focusWindowOn(a);
        foc.addView(button(f ? R.string.pr_focus_off : R.string.pr_focus_on, !f, new View.OnClickListener() {
            @Override public void onClick(View v) { PrayerSettings.setFocusWindowOn(a, !PrayerSettings.focusWindowOn(a)); build(); } }), wrap(12));
        col.addView(foc, wrap(0));
    }

    private View soundButton(int label, final String id, String cur) {
        return button(label, id.equals(cur), new View.OnClickListener() {
            @Override public void onClick(View v) { PrayerSettings.setSound(a, id); build(); } });
    }

    private View offsetRow(final Prayer p) {
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(PrayerNames.label(a, p), 16, R.color.text_primary),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        PrimaryButton minus = new PrimaryButton(a, "−", false);
        minus.setContentDescription(a.getString(R.string.pr_offset_less, PrayerNames.label(a, p)));
        minus.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            PrayerSettings.setOffset(a, p, PrayerSettings.offset(a, p) - 1); changed(); } });
        int val = PrayerSettings.offset(a, p);
        TextView t = text((val > 0 ? "+" : "") + val, 16, R.color.text_primary);
        t.setGravity(Gravity.CENTER);
        t.setMinWidth(Ui.dp(a, 48));
        PrimaryButton plus = new PrimaryButton(a, "+", false);
        plus.setContentDescription(a.getString(R.string.pr_offset_more, PrayerNames.label(a, p)));
        plus.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            PrayerSettings.setOffset(a, p, PrayerSettings.offset(a, p) + 1); changed(); } });
        row.addView(minus, new LinearLayout.LayoutParams(Ui.dp(a, 56), Ui.dp(a, 48)));
        row.addView(t);
        row.addView(plus, new LinearLayout.LayoutParams(Ui.dp(a, 56), Ui.dp(a, 48)));
        return row;
    }

    /** Shown only after the explanation above and an explicit tap. */
    private void openExactAlarmSettings() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + a.getPackageName())));
            } else {
                Toast.makeText(a, R.string.pr_exact_ok, Toast.LENGTH_SHORT).show();
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, R.string.pr_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override public View view() {
        // re-evaluate permission state when coming back from the system screen
        sv.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) { build(); PrayerAlarms.scheduleNext(a.getApplicationContext()); }
            @Override public void onViewDetachedFromWindow(View v) { }
        });
        return sv;
    }
}

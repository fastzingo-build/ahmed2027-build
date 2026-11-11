package com.ahmed2027.app.ui;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.view.View;
import android.widget.TimePicker;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.ActivityLog;
import com.ahmed2027.app.core.Items;
import com.ahmed2027.app.focus.FocusMode;
import com.ahmed2027.app.focus.QuietAlarms;
import com.ahmed2027.app.focus.QuietPeriods;

import java.util.List;

/** Focus sessions, quiet periods and Do Not Disturb (explicit access, honest fallback). */
public final class FocusScreen extends ScreenBase {
    public FocusScreen(Activity a, Host host) {
        super(a, host, R.string.focus_title);
        build();
    }

    /** Shared by Work (dev session) and Focus. Logs the minutes actually spent (at least 1) when the dialog closes. */
    static void runSession(final Activity a, final int minutes, final boolean dnd, final Runnable after) {
        final boolean held = dnd && FocusMode.engage(a, FocusMode.REASON_SESSION);
        if (dnd && !held) Toast.makeText(a, R.string.focus_no_dnd_toast, Toast.LENGTH_LONG).show();
        final long start = com.ahmed2027.app.core.Clock.elapsed();
        final boolean[] finished = {false};
        Dialogs.timer(a, R.string.focus_session, new String[]{a.getString(R.string.focus_line)}, minutes * 60, minutes * 60,
            new Runnable() { @Override public void run() { finished[0] = true; } },
            new Runnable() { @Override public void run() {
                int spent = finished[0] ? minutes : (int) Math.min(minutes, (com.ahmed2027.app.core.Clock.elapsed() - start) / 60000L);
                ActivityLog.log(a, ActivityLog.FOCUS, spent);
                if (held) FocusMode.release(a, FocusMode.REASON_SESSION);
                if (after != null) after.run();
            } });
    }

    private void build() {
        clearBody();
        boolean access = FocusMode.hasAccess(a);
        Card dnd = new Card(a);
        dnd.addView(text(a.getString(R.string.focus_dnd_title), 17, R.color.text_primary));
        dnd.addView(text(a.getString(access ? R.string.focus_dnd_on : R.string.focus_dnd_off), 14, R.color.text_secondary));
        col.addView(dnd, wrap(8));
        if (!access) {
            col.addView(text(a.getString(R.string.focus_dnd_explain), 13, R.color.text_secondary), wrap(8));
            col.addView(button(R.string.focus_dnd_grant, true, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { a.startActivity(FocusMode.accessIntent()); } catch (ActivityNotFoundException e) { Toast.makeText(a, R.string.learn_no_app, Toast.LENGTH_LONG).show(); }
                } }), wrap(8));
            col.addView(text(a.getString(R.string.focus_fallback), 13, R.color.warning), wrap(8));
        } else {
            col.addView(text(a.getString(R.string.focus_allowed), 13, R.color.text_secondary), wrap(8));
        }

        col.addView(new SectionTitle(a, a.getString(R.string.focus_session)));
        final int[] mins = {25, 50, 90};
        for (final int m : mins) {
            PrimaryButton b = new PrimaryButton(a, a.getString(R.string.focus_start_min, m), m == 25);
            b.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { runSession(a, m, true, new Runnable() { @Override public void run() { build(); } }); } });
            col.addView(b, wrap(8));
        }
        col.addView(button(R.string.focus_custom, false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.prompt(a, R.string.focus_custom, R.string.focus_custom_hint, false, true, null, new Dialogs.Text() {
                    @Override public void on(String s) {
                        try { int m = Integer.parseInt(s.trim()); if (m >= 5 && m <= 240) runSession(a, m, true, new Runnable() { @Override public void run() { build(); } });
                            else Toast.makeText(a, R.string.focus_custom_range, Toast.LENGTH_SHORT).show(); } catch (NumberFormatException ignored) { }
                    } });
            } }), wrap(8));
        col.addView(text(a.getString(R.string.focus_week, ActivityLog.lastDays(a, ActivityLog.FOCUS, 7)), 14, R.color.text_secondary), wrap(8));

        col.addView(new SectionTitle(a, a.getString(R.string.quiet_title)));
        col.addView(text(a.getString(R.string.quiet_explain), 13, R.color.text_secondary), wrap(0));
        List<Items.Item> qs = Items.list(a, QuietAlarms.TYPE, null, 50);
        for (final Items.Item q : qs) {
            final boolean on = "on".equals(q.status);
            col.addView(navCard(q.title, a.getString(on ? R.string.quiet_on : R.string.quiet_off), new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Dialogs.choose(a, q.title, new String[]{a.getString(on ? R.string.quiet_turn_off : R.string.quiet_turn_on), a.getString(R.string.learn_remove)}, new Dialogs.Index() {
                        @Override public void on(int i) {
                            if (i == 0) Items.setStatus(a, q.id, on ? "off" : "on"); else Items.delete(a, q.id);
                            QuietAlarms.apply(a); build(); } });
                } }), wrap(8));
        }
        col.addView(button(R.string.quiet_add, false, new View.OnClickListener() { @Override public void onClick(View v) { addQuiet(); } }), wrap(8));
    }

    private void addQuiet() {
        pick(R.string.quiet_from, 22, 0, new TimePickerDialog.OnTimeSetListener() {
            @Override public void onTimeSet(TimePicker t, final int h1, final int m1) {
                pick(R.string.quiet_to, 6, 0, new TimePickerDialog.OnTimeSetListener() {
                    @Override public void onTimeSet(TimePicker t2, int h2, int m2) {
                        QuietPeriods.Period p = new QuietPeriods.Period(h1 * 60 + m1, h2 * 60 + m2);
                        if (p.startMin == p.endMin) { Toast.makeText(a, R.string.quiet_same, Toast.LENGTH_LONG).show(); return; }
                        Items.add(a, QuietAlarms.TYPE, p.format(), "on");
                        QuietAlarms.apply(a); build();
                    } });
            } });
    }

    private void pick(int titleRes, int h, int m, TimePickerDialog.OnTimeSetListener l) {
        TimePickerDialog d = new TimePickerDialog(a, l, h, m, true);
        d.setTitle(titleRes);
        d.show();
    }
}

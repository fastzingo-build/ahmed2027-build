package com.ahmed2027.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Db;
import com.ahmed2027.app.prayer.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Today's prayers, countdown, "I prayed" log and a non-shaming week view. */
public final class PrayerScreen extends ScreenBase {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private TextView countdown;
    private String status;         // transient status line (updating / offline / error)
    private boolean statusIsError;

    public PrayerScreen(Activity a, Host host) {
        super(a, host, R.string.grow_prayer);
        build();
        Ticker.attach(sv, 30_000, new Runnable() { @Override public void run() { updateCountdown(); } });
    }

    private void build() {
        clearBody();
        final Locale loc = Locale.getDefault();
        ZonedDateTime now = ZonedDateTime.now(PrayerLocation.ZONE);
        LocalDate today = now.toLocalDate();
        DayTimes day = PrayerRepo.get(a, today);
        NextPrayer.Result nr = NextPrayer.compute(day, PrayerRepo.get(a, today.plusDays(1)), now);

        // header card: next prayer + dates + source
        Card top = new Card(a);
        countdown = text("", 22, R.color.accent);
        countdown.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(countdown);
        String hijri = "";
        try { hijri = DateTimeFormatter.ofPattern("d MMMM y G", loc).format(HijrahDate.from(today)); } catch (RuntimeException ignored) { }
        top.addView(text(DateTimeFormatter.ofPattern("EEEE d MMMM y", loc).format(today)
                + (hijri.isEmpty() ? "" : "\n" + hijri), 14, R.color.text_secondary));
        TextView src = text(sourceLabel(day), 13, day.source == DayTimes.Source.ESTIMATED ? R.color.warning : R.color.text_secondary);
        src.setPadding(0, Ui.dp(a, 8), 0, 0);
        top.addView(src);
        if (status != null) {
            TextView st = text(status, 14, statusIsError ? R.color.warning : R.color.text_primary);
            st.setPadding(0, Ui.dp(a, 8), 0, 0);
            top.addView(st);
        }
        col.addView(top, wrap(16));

        // today's rows
        Map<String, String> logged = loggedStates(today, today);
        col.addView(new SectionTitle(a, a.getString(R.string.pr_today)));
        Card rows = new Card(a);
        for (final Prayer p : Prayer.values()) {
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(Ui.dp(a, 48));
            boolean isCurrent = p == nr.current;
            if (isCurrent) row.setBackground(Ui.rounded(a, a.getColor(R.color.divider), 12));
            row.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), 0);
            TextView name = text(PrayerNames.label(a, p), 17, isCurrent ? R.color.accent : R.color.text_primary);
            if (isCurrent) name.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            String st = p.isPrayer() ? logged.get(today + "|" + PrayerNames.key(p)) : null;
            if (st != null) {
                TextView chip = text(stateLabel(st), 13, R.color.accent);
                chip.setPadding(0, 0, Ui.dp(a, 12), 0);
                row.addView(chip);
            }
            row.addView(text(day.timeOf(p).toString(), 17, R.color.text_primary));
            if (p.isPrayer()) {
                row.setClickable(true);
                row.setContentDescription(PrayerNames.label(a, p) + " " + day.timeOf(p) + ", " + a.getString(R.string.pr_log_hint));
                row.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { logDialog(p); } });
            }
            rows.addView(row);
        }
        TextView hint = text(a.getString(R.string.pr_log_hint), 13, R.color.text_secondary);
        hint.setPadding(0, Ui.dp(a, 8), 0, 0);
        rows.addView(hint);
        col.addView(rows, wrap(0));

        // week view (non-shaming)
        col.addView(new SectionTitle(a, a.getString(R.string.pr_week)));
        col.addView(weekCard(today), wrap(0));

        // actions
        col.addView(button(R.string.pr_update, true, new View.OnClickListener() {
            @Override public void onClick(View v) { update(); } }), wrap(16));
        col.addView(button(R.string.pr_qibla, false, new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new QiblaScreen(a, host).view()); } }), wrap(8));
        col.addView(button(R.string.pr_adhkar, false, new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new AdhkarScreen(a, host).view()); } }), wrap(8));
        col.addView(button(R.string.pr_settings, false, new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new PrayerSettingsScreen(a, host).view()); } }), wrap(8));
        updateCountdown();
    }

    private String sourceLabel(DayTimes d) {
        if (d.source == DayTimes.Source.ESTIMATED) return a.getString(R.string.pr_src_estimated);
        return a.getString(R.string.pr_src_saved);
    }

    private void updateCountdown() {
        if (countdown == null) return;
        PrayerProvider.Next n = PrayerProvider.next(a);
        long mins = Math.max(0, (n.timeWall - Clock.wall() + 59_999) / 60_000);
        countdown.setText(a.getString(R.string.pr_next_in, PrayerNames.label(a, n.prayer), mins / 60, mins % 60));
    }

    private String stateLabel(String s) {
        if ("on_time".equals(s)) return a.getString(R.string.pr_state_on_time);
        if ("late".equals(s)) return a.getString(R.string.pr_state_late);
        return a.getString(R.string.pr_state_missed);
    }

    // ---- log
    private void logDialog(final Prayer p) {
        final String[] states = {"on_time", "late", "missed", null};
        String[] labels = {a.getString(R.string.pr_state_on_time), a.getString(R.string.pr_state_late),
                a.getString(R.string.pr_state_missed_today), a.getString(R.string.pr_state_clear)};
        new AlertDialog.Builder(a).setTitle(PrayerNames.label(a, p)).setItems(labels, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int which) {
                SQLiteDatabase db = Db.get(a).getWritableDatabase();
                String date = LocalDate.now(PrayerLocation.ZONE).toString();
                if (states[which] == null) {
                    db.delete("prayer_log", "local_date=? AND prayer=?", new String[]{date, PrayerNames.key(p)});
                } else {
                    ContentValues v = new ContentValues();
                    v.put("local_date", date); v.put("prayer", PrayerNames.key(p));
                    v.put("state", states[which]); v.put("logged_wall", Clock.wall());
                    db.insertWithOnConflict("prayer_log", null, v, SQLiteDatabase.CONFLICT_REPLACE);
                }
                build();
            }
        }).setNegativeButton(R.string.action_cancel, null).show();
    }

    private Map<String, String> loggedStates(LocalDate from, LocalDate to) {
        Map<String, String> m = new HashMap<>();
        Cursor c = Db.get(a).getReadableDatabase().rawQuery(
                "SELECT local_date, prayer, state FROM prayer_log WHERE local_date>=? AND local_date<=?",
                new String[]{from.toString(), to.toString()});
        try { while (c.moveToNext()) m.put(c.getString(0) + "|" + c.getString(1), c.getString(2)); }
        finally { c.close(); }
        return m;
    }

    private View weekCard(LocalDate today) {
        Card c = new Card(a);
        Map<String, String> logged = loggedStates(today.minusDays(6), today);
        WeekSummary w = new WeekSummary();
        for (String s : logged.values()) w.add(s);
        if (w.logged() == 0) {
            c.addView(text(a.getString(R.string.pr_week_empty), 15, R.color.text_secondary));
            return c;
        }
        c.addView(text(a.getString(R.string.pr_week_summary, w.prayed(), w.onTime), 16, R.color.text_primary));
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int n = 0;
            for (Prayer p : NextPrayer.FIVE) {
                String s = logged.get(d + "|" + PrayerNames.key(p));
                if ("on_time".equals(s) || "late".equals(s)) n++;
            }
            TextView r = text(a.getString(R.string.pr_week_day, DateTimeFormatter.ofPattern("EEE d", Locale.getDefault()).format(d), n), 14, R.color.text_secondary);
            r.setPadding(0, Ui.dp(a, 6), 0, 0);
            c.addView(r);
        }
        c.addView(text(a.getString(R.string.pr_week_gentle), 13, R.color.text_secondary));
        return c;
    }

    // ---- refresh (never blocks the UI)
    private void update() {
        status = a.getString(R.string.pr_updating); statusIsError = false; build();
        IO.execute(new Runnable() { @Override public void run() {
            final PrayerRepo.Result r = PrayerRepo.refresh(a.getApplicationContext(), true);
            PrayerAlarms.scheduleNext(a.getApplicationContext());
            a.runOnUiThread(new Runnable() { @Override public void run() {
                if (a.isDestroyed() || a.isFinishing()) return;
                switch (r) {
                    case OK: status = a.getString(R.string.pr_update_ok); statusIsError = false; break;
                    case OFFLINE: status = a.getString(R.string.pr_update_offline); statusIsError = true; break;
                    default: status = a.getString(R.string.pr_update_error); statusIsError = true; break;
                }
                build();
            } });
        } });
    }
}

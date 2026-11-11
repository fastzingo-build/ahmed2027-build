package com.ahmed2027.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.analytics.AnalyticsData;
import com.ahmed2027.app.analytics.Series;
import com.ahmed2027.app.core.ActivityLog;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.DataWipe;
import com.ahmed2027.app.friendkey.FriendKeyGate;
import com.ahmed2027.app.friendkey.UnlockManager;

import java.time.LocalDate;
import java.util.Map;

/** Local-only summaries, week or month, with custom-drawn charts. Export JSON; delete my data (confirm + Friend Key). */
public final class AnalyticsScreen extends ScreenBase {
    private int days = 7;

    public AnalyticsScreen(Activity a, Host host) {
        super(a, host, R.string.an_title);
        build();
    }

    private void build() {
        clearBody();
        col.addView(text(a.getString(R.string.an_local_note), 13, R.color.text_secondary), wrap(8));
        col.addView(button(days == 7 ? R.string.an_show_month : R.string.an_show_week, false, new View.OnClickListener() {
            @Override public void onClick(View v) { days = days == 7 ? 30 : 7; build(); } }), wrap(8));
        LocalDate end = LocalDate.now(Clock.zone());
        chart(R.string.an_habits, "habits", AnalyticsData.habits(a), end, R.string.an_unit_checks);
        chart(R.string.an_learning, "learn", AnalyticsData.minutes(a, ActivityLog.LEARN), end, R.string.an_unit_min);
        chart(R.string.an_movement, "move", AnalyticsData.minutes(a, ActivityLog.MOVE), end, R.string.an_unit_min);
        chart(R.string.an_focus, "focus", AnalyticsData.minutes(a, ActivityLog.FOCUS), end, R.string.an_unit_min);
        chart(R.string.an_creator, "creator", AnalyticsData.published(a), end, R.string.an_unit_posts);
        chart(R.string.an_social, "social", AnalyticsData.socialSessions(a), end, R.string.an_unit_sessions);

        int[] pr = AnalyticsData.prayers(a, end.minusDays(days - 1).toString(), end.toString());
        Integer pct = Series.onTimePct(pr[0], pr[1]);
        Card c = new Card(a);
        c.addView(text(a.getString(R.string.an_prayer), 16, R.color.text_primary));
        c.addView(text(pct == null ? a.getString(R.string.an_no_data) : a.getString(R.string.an_prayer_value, pct, pr[0], pr[1]), 14, R.color.text_secondary));
        col.addView(c, wrap(12));

        col.addView(new SectionTitle(a, a.getString(R.string.an_your_data)));
        col.addView(button(R.string.an_export, false, new View.OnClickListener() { @Override public void onClick(View v) { host.startExport(); } }), wrap(0));
        col.addView(text(a.getString(R.string.an_delete_note), 12, R.color.text_secondary), wrap(8));
        col.addView(button(R.string.an_delete, false, new View.OnClickListener() { @Override public void onClick(View v) { confirmDelete(); } }), wrap(8));
    }

    private void chart(int titleRes, String key, Map<String, Integer> perDay, LocalDate end, int unitRes) {
        int[] v = Series.fill(perDay, end, days);
        int total = Series.total(v);
        Card c = new Card(a);
        c.addView(text(a.getString(titleRes), 16, R.color.text_primary));
        String summary = total == 0 ? a.getString(R.string.an_no_data)
                : a.getString(R.string.an_summary, total, a.getString(unitRes), Series.activeDays(v), days);
        c.addView(text(summary, 13, R.color.text_secondary));
        BarChartView ch = new BarChartView(a);
        ch.set(v, a.getString(titleRes) + ": " + summary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(a, 72));
        lp.topMargin = Ui.dp(a, 8);
        c.addView(ch, lp);
        col.addView(c, wrap(8));
    }

    private void confirmDelete() {
        new AlertDialog.Builder(a).setTitle(R.string.an_delete).setMessage(R.string.an_delete_confirm)
            .setPositiveButton(R.string.an_delete_yes, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    if (!FriendKeyGate.active(a, UnlockManager.SETTINGS, null)) { FriendKeyGate.request(a, UnlockManager.SETTINGS, null); return; }
                    DataWipe.wipe(a);
                    Toast.makeText(a, R.string.an_deleted, Toast.LENGTH_LONG).show();
                    build();
                } }).setNegativeButton(R.string.action_cancel, null).show();
    }
}

package com.ahmed2027.app.ui;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.ActivityLog;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Items;
import com.ahmed2027.app.growth.FitnessPlan;
import com.ahmed2027.app.growth.Progression;

import java.time.LocalDate;

/** Short indoor sessions (5–15 min), rest days, manual walking log, +10 % progression suggestion. No sensors, no permissions. */
public final class FitnessScreen extends ScreenBase {
    private static final int[] SESSION_STEPS = {R.array.workout_0, R.array.workout_1, R.array.workout_2, R.array.workout_3, R.array.workout_4};

    public FitnessScreen(Activity a, Host host) {
        super(a, host, R.string.grow_fitness);
        build();
    }

    private void build() {
        clearBody();
        LocalDate today = LocalDate.now(Clock.zone());
        Card safety = new Card(a);
        safety.addView(text(a.getString(R.string.fit_safety), 14, R.color.warning));
        col.addView(safety, wrap(8));

        boolean[] active3 = new boolean[3];
        for (int i = 0; i < 3; i++) {
            LocalDate d = today.minusDays(3 - i);
            active3[i] = ActivityLog.minutes(a, ActivityLog.MOVE, d, d) > 0;
        }
        int pick = FitnessPlan.suggest(active3, today.toEpochDay(), SESSION_STEPS.length);
        String[] names = a.getResources().getStringArray(R.array.workout_names);
        if (pick == FitnessPlan.REST) {
            col.addView(new SectionTitle(a, a.getString(R.string.fit_today)));
            Card c = new Card(a);
            c.addView(text(a.getString(R.string.fit_rest), 16, R.color.text_primary));
            col.addView(c, wrap(0));
        } else {
            col.addView(new SectionTitle(a, a.getString(R.string.fit_today)));
            col.addView(workoutCard(pick, names), wrap(0));
        }
        col.addView(new SectionTitle(a, a.getString(R.string.fit_all)));
        for (int i = 0; i < SESSION_STEPS.length; i++) col.addView(workoutCard(i, names), wrap(8));

        col.addView(new SectionTitle(a, a.getString(R.string.fit_walk)));
        col.addView(button(R.string.fit_log_walk, false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.prompt(a, R.string.fit_log_walk, R.string.fit_minutes_hint, false, true, null, new Dialogs.Text() {
                    @Override public void on(String s) {
                        try { int m = Integer.parseInt(s.trim()); if (m > 0 && m <= 300) {
                            ActivityLog.log(a, ActivityLog.MOVE, m); Items.add(a, "walk", String.valueOf(m), LocalDate.now(Clock.zone()).toString()); build(); } }
                        catch (NumberFormatException ignored) { }
                    } });
            } }), wrap(0));

        int thisWeek = ActivityLog.lastDays(a, ActivityLog.MOVE, 7);
        int lastWeek = ActivityLog.minutes(a, ActivityLog.MOVE, today.minusDays(13), today.minusDays(7));
        col.addView(text(a.getString(R.string.fit_week, thisWeek), 15, R.color.text_primary), wrap(12));
        col.addView(text(a.getString(R.string.fit_goal_suggest, Progression.suggest(lastWeek)), 14, R.color.text_secondary), wrap(4));
        col.addView(text(a.getString(R.string.fit_last14, ActivityLog.lastDays(a, ActivityLog.MOVE, 14)), 14, R.color.text_secondary), wrap(4));
        col.addView(text(a.getString(R.string.fit_no_steps), 12, R.color.text_secondary), wrap(12));
    }

    private View workoutCard(final int idx, String[] names) {
        int minutes = a.getResources().getStringArray(SESSION_STEPS[idx]).length;
        return navCard(names[idx], a.getString(R.string.fit_minutes, minutes), new View.OnClickListener() {
            @Override public void onClick(View v) { start(idx); } });
    }

    private void start(int idx) {
        final String[] steps = a.getResources().getStringArray(SESSION_STEPS[idx]);
        Dialogs.timer(a, R.string.grow_fitness, steps, steps.length * 60, 60, new Runnable() {
            @Override public void run() {
                ActivityLog.log(a, ActivityLog.MOVE, steps.length);
                Items.add(a, "workout", LocalDate.now(Clock.zone()).toString(), "done");
                Toast.makeText(a, R.string.fit_done, Toast.LENGTH_LONG).show();
            } });
    }
}

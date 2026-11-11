package com.ahmed2027.app.ui;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Items;
import com.ahmed2027.app.growth.Messages;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Rule-based support tools. No AI, no diagnosis, no judgement. A visible note says this is not medical advice.
 */
public final class PsychologyScreen extends ScreenBase {
    public PsychologyScreen(Activity a, Host host) {
        super(a, host, R.string.grow_calm);
        build();
    }

    private void build() {
        clearBody();
        Card note = new Card(a);
        note.addView(text(a.getString(R.string.psy_not_medical), 14, R.color.warning));
        col.addView(note, wrap(16));

        Card msg = new Card(a);
        msg.addView(text(Messages.pick(a), 17, R.color.text_primary));
        col.addView(msg, wrap(12));

        col.addView(new SectionTitle(a, a.getString(R.string.psy_tools)));
        col.addView(button(R.string.psy_two_min, true, new View.OnClickListener() { @Override public void onClick(View v) { twoMinute(); } }), wrap(0));
        col.addView(button(R.string.psy_urge, false, new View.OnClickListener() { @Override public void onClick(View v) { urge(); } }), wrap(8));
        col.addView(button(R.string.psy_replace, false, new View.OnClickListener() { @Override public void onClick(View v) { replacement(); } }), wrap(8));
        col.addView(button(R.string.psy_trigger_add, false, new View.OnClickListener() { @Override public void onClick(View v) { trigger(); } }), wrap(8));
        col.addView(button(R.string.psy_recovery, false, new View.OnClickListener() { @Override public void onClick(View v) { recovery(); } }), wrap(8));
        col.addView(button(R.string.reflect_evening, false, new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new ReflectionScreen(a, host, false).view()); } }), wrap(8));
        boolean friday = LocalDate.now(Clock.zone()).getDayOfWeek() == DayOfWeek.FRIDAY;
        col.addView(button(R.string.reflect_weekly, friday, new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new ReflectionScreen(a, host, true).view()); } }), wrap(8));
        if (friday) col.addView(text(a.getString(R.string.reflect_friday), 13, R.color.accent), wrap(4));

        List<Items.Item> triggers = Items.query(a, "trigger", null, "created_ts DESC", 5);
        if (!triggers.isEmpty()) {
            col.addView(new SectionTitle(a, a.getString(R.string.psy_trigger_recent)));
            Card c = new Card(a);
            for (Items.Item it : triggers) {
                c.addView(text(it.title, 16, R.color.text_primary));
                c.addView(text(it.body.replace("\n", "  ·  "), 13, R.color.text_secondary));
                c.addView(new View(a), new android.widget.LinearLayout.LayoutParams(1, Ui.dp(a, 10)));
            }
            col.addView(c, wrap(0));
            col.addView(text(a.getString(R.string.psy_trigger_hint), 13, R.color.text_secondary), wrap(8));
        }
    }

    private void twoMinute() {
        String[] acts = a.getResources().getStringArray(R.array.small_start_actions);
        Dialogs.timer(a, R.string.psy_two_min, new String[]{acts[new Random().nextInt(acts.length)]}, 120, 120, null);
    }

    private void urge() {
        Dialogs.timer(a, R.string.psy_urge, a.getResources().getStringArray(R.array.urge_lines), 180, 30, new Runnable() {
            @Override public void run() { Items.add(a, "urge_surfed", "3min", "done"); } });
    }

    private void replacement() {
        final String[] acts = a.getResources().getStringArray(R.array.replace_actions);
        Dialogs.choose(a, a.getString(R.string.psy_replace), acts, new Dialogs.Index() {
            @Override public void on(int i) {
                Dialogs.timer(a, R.string.psy_replace, new String[]{acts[i]}, 120, 120, null);
            } });
    }

    private void trigger() {
        Dialogs.prompt(a, R.string.psy_t_what, R.string.psy_t_what, false, false, null, new Dialogs.Text() {
            @Override public void on(final String what) {
                Dialogs.prompt(a, R.string.psy_t_where, R.string.psy_t_where, false, false, null, new Dialogs.Text() {
                    @Override public void on(final String where) {
                        final String[] feel = a.getResources().getStringArray(R.array.feelings);
                        Dialogs.choose(a, a.getString(R.string.psy_t_feeling), feel, new Dialogs.Index() {
                            @Override public void on(int i) {
                                String when = java.time.LocalDateTime.now(Clock.zone()).withSecond(0).withNano(0).toString().replace('T', ' ');
                                Items.addFull(a, "trigger", what, when + "\n" + where + "\n" + feel[i], "logged", 0, null);
                                Toast.makeText(a, R.string.psy_t_saved, Toast.LENGTH_LONG).show();
                                build();
                            } });
                    } });
            } });
    }

    private void recovery() {
        Dialogs.steps(a, R.string.psy_recovery, a.getResources().getStringArray(R.array.recovery_steps), new Runnable() {
            @Override public void run() {
                Items.add(a, "recovery", LocalDate.now(Clock.zone()).toString(), "done");
                Toast.makeText(a, R.string.psy_recovery_done, Toast.LENGTH_LONG).show();
            } });
    }
}

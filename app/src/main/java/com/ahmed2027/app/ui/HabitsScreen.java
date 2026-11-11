package com.ahmed2027.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Items;
import com.ahmed2027.app.core.Meta;
import com.ahmed2027.app.growth.HabitMath;
import com.ahmed2027.app.growth.HabitStore;
import com.ahmed2027.app.growth.Messages;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Daily habits in flexible routines. Missed days never show a broken streak: only "restart gently" and comeback badges. */
public final class HabitsScreen extends ScreenBase {
    private static final String[] ROUTINES = {"morning", "evening", "anytime"};
    private static final int[] ROUTINE_LABEL = {R.string.routine_morning, R.string.routine_evening, R.string.routine_anytime};

    public HabitsScreen(Activity a, Host host) {
        super(a, host, R.string.grow_habits);
        build();
    }

    private void build() {
        clearBody();
        LocalDate today = LocalDate.now(Clock.zone());
        List<Items.Item> habits = HabitStore.active(a);

        int weekDone = 0;
        for (Items.Item h : habits) weekDone += HabitMath.weekDone(HabitStore.doneDates(a, h.id), today);
        Card top = new Card(a);
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        HabitRing ring = new HabitRing(a);
        ring.set(weekDone, habits.size() * 7);
        row.addView(ring);
        TextView t = text(habits.isEmpty() ? a.getString(R.string.habits_empty)
                : a.getString(R.string.habits_week, weekDone, habits.size() * 7), 15, R.color.text_primary);
        t.setPadding(Ui.dp(a, 16), 0, 0, 0);
        row.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(row);
        top.addView(text(Messages.pick(a), 14, R.color.text_secondary), wrap(12));
        col.addView(top, wrap(16));

        for (int r = 0; r < ROUTINES.length; r++) {
            Card card = null;
            for (final Items.Item h : habits) {
                if (!ROUTINES[r].equals(h.meta().get("routine", "anytime"))) continue;
                if (card == null) {
                    col.addView(new SectionTitle(a, a.getString(ROUTINE_LABEL[r])));
                    card = new Card(a);
                    col.addView(card, wrap(0));
                }
                card.addView(habitRow(h, today));
            }
        }

        col.addView(text(a.getString(R.string.habits_tiny_hint), 13, R.color.text_secondary), wrap(16));
        col.addView(button(R.string.habit_add, true, new View.OnClickListener() {
            @Override public void onClick(View v) { addHabit(); } }), wrap(8));
    }

    private View habitRow(final Items.Item h, final LocalDate today) {
        Set<LocalDate> done = HabitStore.doneDates(a, h.id);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        CheckBox cb = new CheckBox(a);
        cb.setText(h.title);
        cb.setTextColor(a.getColor(R.color.text_primary));
        cb.setTextSize(16);
        cb.setMinHeight(Ui.dp(a, 48));
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(a.getColor(R.color.accent)));
        cb.setChecked(done.contains(today));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) { HabitStore.setDone(a, h.id, today, on); build(); }
        });
        cb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { confirmArchive(h); return true; }
        });
        box.addView(cb);
        int run = HabitMath.run(done, today);
        boolean back = HabitMath.comeback(done, today);
        if (back || run >= 2) {
            TextView s = text(back ? a.getString(R.string.habit_comeback) : a.getString(R.string.habit_run, run), 13,
                    back ? R.color.accent : R.color.text_secondary);
            s.setPadding(Ui.dp(a, 48), 0, 0, Ui.dp(a, 4));
            box.addView(s);
        } else if (!done.isEmpty() && !done.contains(today) && !done.contains(today.minusDays(1))) {
            TextView s = text(a.getString(R.string.habit_restart), 13, R.color.text_secondary);
            s.setPadding(Ui.dp(a, 48), 0, 0, Ui.dp(a, 4));
            box.addView(s);
        }
        return box;
    }

    private void addHabit() {
        Dialogs.prompt(a, R.string.habit_add, R.string.habit_hint, false, false, null, new Dialogs.Text() {
            @Override public void on(final String title) {
                String[] labels = new String[ROUTINES.length];
                for (int i = 0; i < labels.length; i++) labels[i] = a.getString(ROUTINE_LABEL[i]);
                Dialogs.choose(a, a.getString(R.string.habit_routine), labels, new Dialogs.Index() {
                    @Override public void on(int i) { HabitStore.add(a, title, ROUTINES[i]); build(); }
                });
            }
        });
    }

    private void confirmArchive(final Items.Item h) {
        new AlertDialog.Builder(a).setTitle(h.title).setMessage(R.string.habit_archive_q)
                .setPositiveButton(R.string.habit_archive, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { HabitStore.archive(a, h.id); build(); }
                }).setNegativeButton(R.string.action_cancel, null).show();
    }
}

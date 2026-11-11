package com.ahmed2027.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.ActivityLog;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Items;
import com.ahmed2027.app.core.Meta;
import com.ahmed2027.app.growth.Leitner;
import com.ahmed2027.app.growth.ReadingPlan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** English A1→A2 plan, vocabulary (Leitner), reading plan, thinking exercises, goals. All data stays on the device. */
public final class PersonalDevScreen extends ScreenBase {
    public PersonalDevScreen(Activity a, Host host) {
        super(a, host, R.string.grow_personal);
        seedVocab();
        build();
    }

    private long today() { return LocalDate.now(Clock.zone()).toEpochDay(); }

    private void seedVocab() {
        if (Items.count(a, "vocab", null) > 0) return;
        for (String line : a.getResources().getStringArray(R.array.vocab_seed)) {
            int i = line.indexOf('|');
            if (i < 0) continue;
            Items.addFull(a, "vocab", line.substring(0, i), line.substring(i + 1), "1", today(), null);
        }
    }

    private void build() {
        clearBody();
        col.addView(button(R.string.pd_log_minutes, false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.prompt(a, R.string.pd_log_minutes, R.string.pd_minutes_hint, false, true, null, new Dialogs.Text() {
                    @Override public void on(String s) {
                        try { int m = Integer.parseInt(s.trim()); if (m > 0 && m <= 600) { ActivityLog.log(a, ActivityLog.LEARN, m);
                            Toast.makeText(a, R.string.pd_logged, Toast.LENGTH_SHORT).show(); build(); } } catch (NumberFormatException ignored) { }
                    } });
            } }), wrap(8));
        col.addView(text(a.getString(R.string.pd_week_minutes, ActivityLog.lastDays(a, ActivityLog.LEARN, 7)), 14, R.color.text_secondary), wrap(8));
        englishPlan();
        vocab();
        reading();
        exercises();
        goals();
    }

    // ---- English plan ----
    private void englishPlan() {
        col.addView(new SectionTitle(a, a.getString(R.string.pd_english)));
        col.addView(text(a.getString(R.string.pd_english_intro), 13, R.color.text_secondary), wrap(0));
        int[] arrays = {R.array.english_w1, R.array.english_w2, R.array.english_w3, R.array.english_w4};
        for (int w = 0; w < arrays.length; w++) {
            Card card = new Card(a);
            card.addView(text(a.getString(R.string.pd_week, w + 1), 16, R.color.accent));
            String[] tasks = a.getResources().getStringArray(arrays[w]);
            for (int t = 0; t < tasks.length; t++) {
                final String key = "w" + (w + 1) + "t" + t;
                final List<Items.Item> found = Items.query(a, "plan_task", null, "id ASC", 500);
                Items.Item hit = null;
                for (Items.Item it : found) if (it.title.equals(key)) hit = it;
                final Items.Item cur = hit;
                CheckBox cb = new CheckBox(a);
                cb.setText(tasks[t]);
                cb.setTextColor(a.getColor(R.color.text_primary));
                cb.setTextSize(15);
                cb.setMinHeight(Ui.dp(a, 48));
                cb.setButtonTintList(android.content.res.ColorStateList.valueOf(a.getColor(R.color.accent)));
                cb.setChecked(cur != null && "done".equals(cur.status));
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override public void onCheckedChanged(CompoundButton b, boolean on) {
                        if (cur == null) Items.addFull(a, "plan_task", key, "", on ? "done" : "open", 0, null);
                        else Items.setStatus(a, cur.id, on ? "done" : "open");
                    } });
                card.addView(cb);
            }
            col.addView(card, wrap(8));
        }
    }

    // ---- Vocabulary ----
    private List<Items.Item> dueCards() {
        List<Items.Item> out = new ArrayList<Items.Item>();
        long t = today();
        for (Items.Item it : Items.query(a, "vocab", null, "due_ts ASC", 1000))
            if (Leitner.isDue(it.dueTs, t)) out.add(it);
        return out;
    }

    private void vocab() {
        col.addView(new SectionTitle(a, a.getString(R.string.pd_vocab)));
        final List<Items.Item> due = dueCards();
        Card c = new Card(a);
        c.addView(text(a.getString(R.string.pd_vocab_due, due.size(), Items.count(a, "vocab", null)), 15, R.color.text_primary));
        col.addView(c, wrap(0));
        if (!due.isEmpty()) col.addView(button(R.string.pd_review, true, new View.OnClickListener() {
            @Override public void onClick(View v) { review(due, 0); } }), wrap(8));
        col.addView(button(R.string.pd_add_word, false, new View.OnClickListener() {
            @Override public void onClick(View v) { addWord(); } }), wrap(8));
    }

    private void addWord() {
        Dialogs.prompt(a, R.string.pd_add_word, R.string.pd_word_hint, false, false, null, new Dialogs.Text() {
            @Override public void on(final String w) {
                Dialogs.prompt(a, R.string.pd_add_word, R.string.pd_translation_hint, false, false, null, new Dialogs.Text() {
                    @Override public void on(String tr) { Items.addFull(a, "vocab", w, tr, "1", today(), null); build(); } });
            } });
    }

    private void review(final List<Items.Item> cards, final int i) {
        if (i >= cards.size()) { Toast.makeText(a, R.string.pd_review_done, Toast.LENGTH_LONG).show(); ActivityLog.log(a, ActivityLog.LEARN, 3); build(); return; }
        final Items.Item it = cards.get(i);
        new AlertDialog.Builder(a).setTitle(a.getString(R.string.pd_card_of, i + 1, cards.size())).setMessage(it.title)
            .setPositiveButton(R.string.pd_show, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) { answer(cards, i); } })
            .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) { build(); } })
            .setCancelable(false).show();
    }

    private void answer(final List<Items.Item> cards, final int i) {
        final Items.Item it = cards.get(i);
        final int box = parse(it.status);
        new AlertDialog.Builder(a).setTitle(it.title).setMessage(it.body)
            .setPositiveButton(R.string.pd_knew, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) { grade(it, box, true); review(cards, i + 1); } })
            .setNegativeButton(R.string.pd_again, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) { grade(it, box, false); review(cards, i + 1); } })
            .setCancelable(false).show();
    }

    private int parse(String s) { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 1; } }

    private void grade(Items.Item it, int box, boolean ok) {
        int nb = Leitner.nextBox(box, ok);
        Items.setStatus(a, it.id, String.valueOf(nb));
        Items.setDue(a, it.id, Leitner.dueDay(today(), nb));
    }

    // ---- Reading ----
    private void reading() {
        col.addView(new SectionTitle(a, a.getString(R.string.pd_reading)));
        List<Items.Item> books = Items.list(a, "book", "active", 20);
        for (final Items.Item b : books) {
            int total = (int) b.meta().getLong("total", 0), perDay = (int) b.meta().getLong("per_day", 10);
            int read = (int) parseL(b.body);
            Card c = new Card(a);
            c.addView(text(b.title, 17, R.color.text_primary));
            c.addView(text(a.getString(R.string.pd_book_progress, read, total, ReadingPlan.percent(total, read),
                    ReadingPlan.daysToFinish(total, read, perDay)), 14, R.color.text_secondary));
            c.setClickable(true);
            c.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { logPages(b); } });
            col.addView(c, wrap(8));
        }
        col.addView(button(R.string.pd_add_book, false, new View.OnClickListener() {
            @Override public void onClick(View v) { addBook(); } }), wrap(8));
    }

    private long parseL(String s) { try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0; } }

    private void addBook() {
        Dialogs.prompt(a, R.string.pd_add_book, R.string.pd_book_title, false, false, null, new Dialogs.Text() {
            @Override public void on(final String title) {
                Dialogs.prompt(a, R.string.pd_add_book, R.string.pd_book_pages, false, true, null, new Dialogs.Text() {
                    @Override public void on(String pages) {
                        long total = Math.max(1, parseL(pages));
                        Items.addFull(a, "book", title, "0", "active", 0, new Meta().set("total", total).set("per_day", 10).toString());
                        build();
                    } });
            } });
    }

    private void logPages(final Items.Item b) {
        Dialogs.prompt(a, R.string.pd_pages_today, R.string.pd_pages_today, false, true, null, new Dialogs.Text() {
            @Override public void on(String s) {
                long add = parseL(s);
                if (add <= 0) return;
                Items.setBody(a, b.id, String.valueOf(parseL(b.body) + add));
                ActivityLog.log(a, ActivityLog.LEARN, (int) Math.min(120, add * 2));
                build();
            } });
    }

    // ---- Exercises ----
    private void exercises() {
        col.addView(new SectionTitle(a, a.getString(R.string.pd_exercises)));
        final String[] titles = a.getResources().getStringArray(R.array.exercise_titles);
        final String[] texts = a.getResources().getStringArray(R.array.exercise_texts);
        for (int i = 0; i < titles.length; i++) {
            final int k = i;
            col.addView(navCard(titles[i], a.getString(R.string.pd_exercise_sub), new View.OnClickListener() {
                @Override public void onClick(View v) { Dialogs.message(a, R.string.pd_exercises, titles[k] + "\n\n" + texts[k]); } }), wrap(8));
        }
    }

    // ---- Goals ----
    private void goals() {
        col.addView(new SectionTitle(a, a.getString(R.string.pd_goals)));
        for (final Items.Item g : Items.list(a, "goal", "active", 20)) {
            Card c = new Card(a);
            c.addView(text(g.title, 17, R.color.text_primary));
            String[] ms = g.body.isEmpty() ? new String[0] : g.body.split("\n");
            int done = 0;
            for (String m : ms) if (m.startsWith("1|")) done++;
            c.addView(text(a.getString(R.string.pd_goal_progress, done, ms.length), 13, R.color.text_secondary));
            c.setClickable(true);
            c.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { goalMenu(g); } });
            col.addView(c, wrap(8));
        }
        col.addView(button(R.string.pd_add_goal, false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.prompt(a, R.string.pd_add_goal, R.string.pd_goal_hint, false, false, null, new Dialogs.Text() {
                    @Override public void on(String s) { Items.addFull(a, "goal", s, "", "active", 0, null); build(); } });
            } }), wrap(8));
    }

    private void goalMenu(final Items.Item g) {
        final String[] ms = g.body.isEmpty() ? new String[0] : g.body.split("\n");
        final String[] labels = new String[ms.length + 2];
        for (int i = 0; i < ms.length; i++) labels[i] = (ms[i].startsWith("1|") ? "✓ " : "○ ") + ms[i].substring(2);
        labels[ms.length] = a.getString(R.string.pd_add_milestone);
        labels[ms.length + 1] = a.getString(R.string.pd_goal_done);
        Dialogs.choose(a, g.title, labels, new Dialogs.Index() {
            @Override public void on(int i) {
                if (i < ms.length) {
                    ms[i] = (ms[i].startsWith("1|") ? "0|" : "1|") + ms[i].substring(2);
                    Items.setBody(a, g.id, join(ms)); build();
                } else if (i == ms.length) {
                    Dialogs.prompt(a, R.string.pd_add_milestone, R.string.pd_milestone_hint, false, false, null, new Dialogs.Text() {
                        @Override public void on(String s) {
                            String body = g.body.isEmpty() ? "0|" + s : g.body + "\n0|" + s;
                            Items.setBody(a, g.id, body); build(); } });
                } else { Items.setStatus(a, g.id, "done"); build(); }
            } });
    }

    private String join(String[] ms) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ms.length; i++) { if (i > 0) sb.append('\n'); sb.append(ms[i]); }
        return sb.toString();
    }
}

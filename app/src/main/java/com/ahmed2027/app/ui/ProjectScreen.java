package com.ahmed2027.app.ui;

import android.app.Activity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Items;
import com.ahmed2027.app.core.Meta;

import java.util.List;

/** One project: notes, tasks, bugs, features, milestones (all items type "ptask", meta project/kind/prio). */
public final class ProjectScreen extends ScreenBase {
    private static final String[] KINDS = {"task", "bug", "feature", "milestone"};
    private static final int[] KIND_LABEL = {R.string.pk_task, R.string.pk_bug, R.string.pk_feature, R.string.pk_milestone};
    private static final int[] KIND_ADD = {R.string.pk_add_task, R.string.pk_add_bug, R.string.pk_add_feature, R.string.pk_add_milestone};
    private final long projectId;

    public ProjectScreen(Activity a, Host host, long projectId, String name) {
        super(a, host, R.string.work_project);
        this.projectId = projectId;
        col.addView(text(name, 20, R.color.text_primary), wrap(4));
        build();
    }

    @Override protected int headerCount() { return 3; }

    private void build() {
        clearBody();
        final Items.Item p = Items.get(a, projectId);
        Card notes = new Card(a);
        notes.addView(text(a.getString(R.string.pn_notes), 14, R.color.accent));
        notes.addView(text(p == null || p.body.isEmpty() ? a.getString(R.string.pn_empty) : p.body, 15, R.color.text_primary));
        notes.setClickable(true);
        notes.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.prompt(a, R.string.pn_notes, R.string.pn_notes, true, false, p == null ? "" : p.body, new Dialogs.Text() {
                    @Override public void on(String s) { Items.setBody(a, projectId, s); build(); } });
            } });
        col.addView(notes, wrap(12));

        List<Items.Item> all = Items.query(a, "ptask", null, "status ASC, id ASC", 500);
        for (int k = 0; k < KINDS.length; k++) {
            col.addView(new SectionTitle(a, a.getString(KIND_LABEL[k])));
            Card c = new Card(a);
            int n = 0;
            for (final Items.Item it : all) {
                Meta m = it.meta();
                if (m.getLong("project", -1) != projectId || !KINDS[k].equals(m.get("kind", "task"))) continue;
                n++;
                c.addView(row(it, m));
            }
            if (n == 0) c.addView(text(a.getString(R.string.pn_none), 14, R.color.text_secondary));
            col.addView(c, wrap(0));
            final String kind = KINDS[k];
            col.addView(button(KIND_ADD[k], false, new View.OnClickListener() { @Override public void onClick(View v) { add(kind); } }), wrap(8));
        }
    }

    private View row(final Items.Item it, Meta m) {
        long prio = m.getLong("prio", 2);
        CheckBox cb = new CheckBox(a);
        cb.setText("P" + prio + "  " + it.title + ("doing".equals(it.status) ? "  (" + a.getString(R.string.pn_doing) + ")" : ""));
        cb.setTextColor(a.getColor(R.color.text_primary));
        cb.setTextSize(15);
        cb.setMinHeight(Ui.dp(a, 48));
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(a.getColor(R.color.accent)));
        cb.setChecked("done".equals(it.status));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) { Items.setStatus(a, it.id, on ? "done" : "open"); build(); } });
        cb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { menu(it); return true; } });
        return cb;
    }

    private void menu(final Items.Item it) {
        Dialogs.choose(a, it.title, new String[]{a.getString(R.string.pn_set_doing), a.getString(R.string.pn_set_prio), a.getString(R.string.learn_remove)}, new Dialogs.Index() {
            @Override public void on(int i) {
                if (i == 0) { Items.setStatus(a, it.id, "doing"); build(); }
                else if (i == 1) Dialogs.choose(a, a.getString(R.string.pn_set_prio), new String[]{"P1 " + a.getString(R.string.pn_high), "P2", "P3 " + a.getString(R.string.pn_low)}, new Dialogs.Index() {
                    @Override public void on(int p) { Items.setMeta(a, it.id, it.meta().set("prio", p + 1)); build(); } });
                else { Items.delete(a, it.id); build(); }
            } });
    }

    private void add(final String kind) {
        Dialogs.prompt(a, R.string.pn_add, R.string.pn_title_hint, false, false, null, new Dialogs.Text() {
            @Override public void on(final String title) {
                Dialogs.choose(a, a.getString(R.string.pn_set_prio), new String[]{"P1 " + a.getString(R.string.pn_high), "P2", "P3 " + a.getString(R.string.pn_low)}, new Dialogs.Index() {
                    @Override public void on(int p) {
                        Items.addFull(a, "ptask", title, "", "open", 0, new Meta().set("project", projectId).set("kind", kind).set("prio", p + 1).toString());
                        build(); } });
            } });
    }
}

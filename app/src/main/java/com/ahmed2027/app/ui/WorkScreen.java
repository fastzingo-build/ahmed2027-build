package com.ahmed2027.app.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Items;

import java.time.LocalDate;
import java.util.List;

/** Work hub: projects, dev session timer, journal, repo links, AI-tool shortcuts, technical resources. */
public final class WorkScreen extends ScreenBase {
    private SharedPreferences prefs() { return a.getSharedPreferences("work", Context.MODE_PRIVATE); }

    public WorkScreen(Activity a, Host host) {
        super(a, host, R.string.work_title);
        build();
    }

    private void build() {
        clearBody();
        // Projects
        col.addView(new SectionTitle(a, a.getString(R.string.work_projects)));
        for (final Items.Item p : Items.list(a, "project", "active", 50)) {
            int open = 0;
            for (Items.Item t : Items.query(a, "ptask", "open", "id ASC", 500)) if (t.meta().getLong("project", -1) == p.id) open++;
            col.addView(navCard(p.title, a.getString(R.string.work_open_items, open), new View.OnClickListener() {
                @Override public void onClick(View v) { host.push(new ProjectScreen(a, host, p.id, p.title).view()); } }), wrap(8));
        }
        col.addView(button(R.string.work_add_project, false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.prompt(a, R.string.work_add_project, R.string.work_project_hint, false, false, null, new Dialogs.Text() {
                    @Override public void on(String s) { Items.addFull(a, "project", s, "", "active", 0, null); build(); } });
            } }), wrap(8));

        // Dev session
        col.addView(new SectionTitle(a, a.getString(R.string.work_dev)));
        final int work = prefs().getInt("dev_work", 25), brk = prefs().getInt("dev_break", 5);
        col.addView(text(a.getString(R.string.work_dev_cfg, work, brk), 14, R.color.text_secondary), wrap(0));
        col.addView(button(R.string.work_dev_start, true, new View.OnClickListener() {
            @Override public void onClick(View v) {
                FocusScreen.runSession(a, work, false, new Runnable() {
                    @Override public void run() {
                        Dialogs.timer(a, R.string.work_break, new String[]{a.getString(R.string.work_break_line)}, brk * 60, brk * 60, null);
                    } });
            } }), wrap(8));
        col.addView(button(R.string.work_dev_cfg_btn, false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.prompt(a, R.string.work_dev_cfg_btn, R.string.work_dev_work_hint, false, true, String.valueOf(work), new Dialogs.Text() {
                    @Override public void on(final String w) {
                        Dialogs.prompt(a, R.string.work_dev_cfg_btn, R.string.work_dev_break_hint, false, true, String.valueOf(brk), new Dialogs.Text() {
                            @Override public void on(String b) {
                                try {
                                    int wi = Integer.parseInt(w.trim()), bi = Integer.parseInt(b.trim());
                                    if (wi < 5 || wi > 180 || bi < 1 || bi > 60) { Toast.makeText(a, R.string.focus_custom_range, Toast.LENGTH_LONG).show(); return; }
                                    prefs().edit().putInt("dev_work", wi).putInt("dev_break", bi).apply(); build();
                                } catch (NumberFormatException ignored) { }
                            } });
                    } });
            } }), wrap(8));

        // Journal
        col.addView(new SectionTitle(a, a.getString(R.string.work_journal)));
        col.addView(button(R.string.work_journal_add, false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.prompt(a, R.string.work_journal_add, R.string.work_journal_hint, true, false, null, new Dialogs.Text() {
                    @Override public void on(String s) { Items.addFull(a, "journal", LocalDate.now(Clock.zone()).toString(), s, "saved", 0, null); build(); } });
            } }), wrap(0));
        List<Items.Item> js = Items.query(a, "journal", null, "created_ts DESC", 5);
        if (!js.isEmpty()) {
            Card c = new Card(a);
            for (Items.Item j : js) { c.addView(text(j.title, 13, R.color.accent)); c.addView(text(j.body, 14, R.color.text_primary)); c.addView(new View(a), new android.widget.LinearLayout.LayoutParams(1, Ui.dp(a, 10))); }
            col.addView(c, wrap(8));
        }

        // Repos
        col.addView(new SectionTitle(a, a.getString(R.string.work_repos)));
        linkList("repo", R.string.work_add_repo);
        // AI tools
        col.addView(new SectionTitle(a, a.getString(R.string.work_ai)));
        col.addView(text(a.getString(R.string.work_ai_note), 13, R.color.text_secondary), wrap(0));
        ai(R.string.ai_claude, "com.anthropic.claude", "https://claude.ai");
        ai(R.string.ai_chatgpt, "com.openai.chatgpt", "https://chatgpt.com");
        ai(R.string.ai_gemini, "com.google.android.apps.bard", "https://gemini.google.com");
        // Technical resources
        col.addView(new SectionTitle(a, a.getString(R.string.work_resources)));
        linkList("tech_res", R.string.work_add_resource);
    }

    private void ai(int label, final String pkg, final String url) {
        col.addView(button(label, false, new View.OnClickListener() { @Override public void onClick(View v) { Links.openAppOrWeb(a, pkg, url); } }), wrap(8));
    }

    private void linkList(final String type, int addLabel) {
        for (final Items.Item r : Items.list(a, type, null, 50)) {
            col.addView(navCard(r.title, r.body, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Dialogs.choose(a, r.title, new String[]{a.getString(R.string.work_open), a.getString(R.string.learn_remove)}, new Dialogs.Index() {
                        @Override public void on(int i) { if (i == 0) Links.openUrl(a, r.body); else { Items.delete(a, r.id); build(); } } });
                } }), wrap(8));
        }
        col.addView(button(addLabel, false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.prompt(a, R.string.work_link_title, R.string.work_link_title, false, false, null, new Dialogs.Text() {
                    @Override public void on(final String title) {
                        Dialogs.prompt(a, R.string.work_link_url, R.string.work_link_url, false, false, null, new Dialogs.Text() {
                            @Override public void on(String url) {
                                String u = Links.normalize(url);
                                if (u == null) { Toast.makeText(a, R.string.link_bad, Toast.LENGTH_LONG).show(); return; }
                                Items.addFull(a, type, title, u, "saved", 0, null); build();
                            } });
                    } });
            } }), wrap(8));
    }
}

package com.ahmed2027.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Items;
import com.ahmed2027.app.core.Meta;
import com.ahmed2027.app.creator.Pipeline;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Content pipeline for TikTok / Instagram. No auto-posting, no account access; apps open only through the Shield's rules. */
public final class CreatorScreen extends ScreenBase {
    private static final String TYPE = "content";
    private static final int[] STAGE_LABEL = {R.string.stage_idea, R.string.stage_planning, R.string.stage_recording, R.string.stage_editing, R.string.stage_ready, R.string.stage_published};
    private boolean monthView = false;

    public CreatorScreen(Activity a, Host host) {
        super(a, host, R.string.creator_title);
        build();
    }

    private SharedPreferences prefs() { return a.getSharedPreferences("creator", Context.MODE_PRIVATE); }
    private LocalDate today() { return LocalDate.now(Clock.zone()); }

    private void build() {
        clearBody();
        List<Items.Item> all = Items.query(a, TYPE, null, "id ASC", 500);
        List<String> pubDates = new ArrayList<String>();
        for (Items.Item it : all) if ("published".equals(it.status)) pubDates.add(it.meta().get("pub", ""));
        int goal = prefs().getInt("goal", 3);
        int done = Pipeline.inWeek(pubDates, Pipeline.weekStart(today()));
        Card g = new Card(a);
        g.addView(text(a.getString(R.string.creator_goal, done, goal), 17, R.color.text_primary));
        g.addView(text(a.getString(done >= goal ? R.string.creator_goal_met : R.string.creator_goal_gentle), 13, R.color.text_secondary));
        g.setClickable(true);
        g.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.prompt(a, R.string.creator_goal_set, R.string.creator_goal_set, false, true, null, new Dialogs.Text() {
                    @Override public void on(String s) { try { int n = Integer.parseInt(s.trim()); if (n >= 1 && n <= 21) { prefs().edit().putInt("goal", n).apply(); build(); } } catch (NumberFormatException ignored) { } } });
            } });
        col.addView(g, wrap(8));

        col.addView(button(R.string.creator_add_idea, true, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.prompt(a, R.string.creator_add_idea, R.string.creator_idea_hint, false, false, null, new Dialogs.Text() {
                    @Override public void on(String s) { Items.addFull(a, TYPE, s, "", "idea", 0, new Meta().set("platform", "tiktok").toString()); build(); } });
            } }), wrap(8));

        // Calendar
        col.addView(new SectionTitle(a, a.getString(R.string.creator_calendar)));
        col.addView(button(monthView ? R.string.creator_view_week : R.string.creator_view_month, false, new View.OnClickListener() {
            @Override public void onClick(View v) { monthView = !monthView; build(); } }), wrap(0));
        LocalDate start = monthView ? today().withDayOfMonth(1) : Pipeline.weekStart(today());
        LocalDate end = monthView ? start.plusMonths(1).minusDays(1) : start.plusDays(6);
        Card cal = new Card(a);
        int n = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            for (Items.Item it : all) {
                if (!d.toString().equals(it.meta().get("sched", ""))) continue;
                n++;
                cal.addView(text(d.getDayOfWeek().toString().substring(0, 3) + " " + d.getDayOfMonth() + "  ·  " + it.title + "  (" + a.getString(STAGE_LABEL[Pipeline.index(it.status)]) + ")", 14, R.color.text_primary));
            }
        }
        if (n == 0) cal.addView(text(a.getString(R.string.creator_cal_empty), 14, R.color.text_secondary));
        col.addView(cal, wrap(8));

        // Pipeline
        for (int s = 0; s < Pipeline.STAGES.length; s++) {
            boolean any = false;
            for (final Items.Item it : all) {
                if (!Pipeline.STAGES[s].equals(it.status)) continue;
                if (!any) { col.addView(new SectionTitle(a, a.getString(STAGE_LABEL[s]))); any = true; }
                Meta m = it.meta();
                String sub = a.getString("both".equals(m.get("platform")) ? R.string.plat_both : "instagram".equals(m.get("platform")) ? R.string.plat_instagram : R.string.plat_tiktok)
                        + (m.has("sched") ? "  ·  " + m.get("sched") : "");
                col.addView(navCard(it.title, sub, new View.OnClickListener() { @Override public void onClick(View v) { menu(it); } }), wrap(8));
            }
        }

        col.addView(new SectionTitle(a, a.getString(R.string.creator_open_apps)));
        col.addView(text(a.getString(R.string.creator_open_note), 13, R.color.text_secondary), wrap(0));
        col.addView(button(R.string.social_tiktok, false, new View.OnClickListener() {
            @Override public void onClick(View v) { Links.openAppOnly(a, "com.zhiliaoapp.musically"); } }), wrap(8));
        col.addView(button(R.string.social_instagram, false, new View.OnClickListener() {
            @Override public void onClick(View v) { Links.openAppOnly(a, "com.instagram.android"); } }), wrap(8));
    }

    private void menu(final Items.Item it) {
        final boolean last = Pipeline.isLast(it.status);
        final Meta m = it.meta();
        List<String> labels = new ArrayList<String>();
        labels.add(a.getString(last ? R.string.creator_open_post : R.string.creator_advance));
        labels.add(a.getString(R.string.creator_edit));
        labels.add(a.getString(R.string.creator_set_date));
        labels.add(a.getString(R.string.creator_set_platform));
        labels.add(a.getString(R.string.learn_remove));
        Dialogs.choose(a, it.title, labels.toArray(new String[0]), new Dialogs.Index() {
            @Override public void on(int i) {
                if (i == 0) { if (last) { if (m.has("url")) Links.openUrl(a, m.get("url").replace("%3B", ";").replace("%3D", "=")); else Toast.makeText(a, R.string.creator_no_url, Toast.LENGTH_SHORT).show(); } else advance(it); }
                else if (i == 1) host.push(new ContentEditScreen(a, host, it.id).view());
                else if (i == 2) pickDate(it);
                else if (i == 3) Dialogs.choose(a, a.getString(R.string.creator_set_platform), new String[]{a.getString(R.string.plat_tiktok), a.getString(R.string.plat_instagram), a.getString(R.string.plat_both)}, new Dialogs.Index() {
                    @Override public void on(int p) { Items.setMeta(a, it.id, m.set("platform", p == 0 ? "tiktok" : p == 1 ? "instagram" : "both")); build(); } });
                else { Items.delete(a, it.id); build(); }
            } });
    }

    private void pickDate(final Items.Item it) {
        LocalDate t = today();
        new DatePickerDialog(a, new DatePickerDialog.OnDateSetListener() {
            @Override public void onDateSet(DatePicker p, int y, int mo, int d) {
                Items.setMeta(a, it.id, it.meta().set("sched", LocalDate.of(y, mo + 1, d).toString())); build(); }
        }, t.getYear(), t.getMonthValue() - 1, t.getDayOfMonth()).show();
    }

    private void advance(final Items.Item it) {
        final String next = Pipeline.next(it.status);
        if (!"published".equals(next)) { Items.setStatus(a, it.id, next); build(); return; }
        // Publishing checklist (advisory: you decide), then the post URL.
        final String[] items = a.getResources().getStringArray(R.array.publish_checklist);
        new AlertDialog.Builder(a).setTitle(R.string.creator_checklist)
            .setMultiChoiceItems(items, new boolean[items.length], new DialogInterface.OnMultiChoiceClickListener() {
                @Override public void onClick(DialogInterface d, int which, boolean checked) { } })
            .setPositiveButton(R.string.creator_mark_published, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    Dialogs.prompt(a, R.string.creator_post_url, R.string.creator_post_url_hint, false, false, null, new Dialogs.Text() {
                        @Override public void on(String url) {
                            Meta m = it.meta().set("pub", today().toString());
                            String u = url.trim().isEmpty() ? null : Links.normalize(url);
                            if (u != null) m.set("url", u.replace(";", "%3B").replace("=", "%3D"));
                            Items.setMeta(a, it.id, m);
                            Items.setStatus(a, it.id, "published");
                            build();
                        } });
                } }).setNegativeButton(R.string.action_cancel, null).show();
    }
}

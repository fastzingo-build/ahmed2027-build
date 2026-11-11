package com.ahmed2027.app.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Items;
import com.ahmed2027.app.core.Meta;
import com.ahmed2027.app.learn.YouTubeId;

import java.util.List;

/**
 * Curated learning library. No search, no feed, no Shorts. Seeded entries are official channels (opened in the
 * YouTube app/browser, outside this app); videos you add by link play in a restricted embedded player.
 */
public final class LearnScreen extends ScreenBase {
    private static final String TYPE = "learn_res";
    private static final String[] CATS = {"english", "science", "history", "culture", "technology", "programming", "entrepreneurship", "business"};
    private static final int[] CAT_LABEL = {R.string.cat_english, R.string.cat_science, R.string.cat_history, R.string.cat_culture,
            R.string.cat_technology, R.string.cat_programming, R.string.cat_entrepreneurship, R.string.cat_business};
    // name | url | category index
    private static final Object[][] SEED = {
        {"BBC Learning English", "https://www.youtube.com/@bbclearningenglish", 0},
        {"English with Lucy", "https://www.youtube.com/@EnglishwithLucy", 0},
        {"Kurzgesagt – In a Nutshell", "https://www.youtube.com/@kurzgesagt", 1},
        {"3Blue1Brown", "https://www.youtube.com/@3blue1brown", 1},
        {"Veritasium", "https://www.youtube.com/@veritasium", 1},
        {"SciShow", "https://www.youtube.com/@SciShow", 1},
        {"TED-Ed", "https://www.youtube.com/@TED-Ed", 3},
        {"CrashCourse", "https://www.youtube.com/@crashcourse", 3},
        {"OverSimplified", "https://www.youtube.com/@OverSimplified", 2},
        {"MKBHD", "https://www.youtube.com/@mkbhd", 4},
        {"freeCodeCamp.org", "https://www.youtube.com/@freecodecamp", 5},
        {"CS50", "https://www.youtube.com/@cs50", 5},
        {"Fireship", "https://www.youtube.com/@Fireship", 5},
        {"Y Combinator", "https://www.youtube.com/@ycombinator", 6},
        {"Harvard Business Review", "https://www.youtube.com/@harvardbusinessreview", 7},
    };

    private int filter = 0; // 0 all, 1 fav, 2 later, 3 history

    public LearnScreen(Activity a, Host host) {
        super(a, host, R.string.grow_learn);
        seed();
        build();
    }

    private void seed() {
        if (Items.count(a, TYPE, null) > 0) return;
        for (Object[] s : SEED)
            Items.addFull(a, TYPE, (String) s[0], (String) s[1], CATS[(Integer) s[2]], 0,
                    new Meta().set("kind", "channel").set("seed", "1").set("verify", "1").toString());
    }

    private void build() {
        clearBody();
        col.addView(text(a.getString(R.string.learn_intro), 14, R.color.text_secondary), wrap(8));
        Card warn = new Card(a);
        warn.addView(text(a.getString(R.string.learn_limit), 13, R.color.warning));
        col.addView(warn, wrap(8));

        String[] f = {a.getString(R.string.learn_f_all), a.getString(R.string.learn_f_fav), a.getString(R.string.learn_f_later), a.getString(R.string.learn_f_history)};
        col.addView(button(R.string.learn_filter, false, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Dialogs.choose(a, a.getString(R.string.learn_filter), new String[]{a.getString(R.string.learn_f_all), a.getString(R.string.learn_f_fav),
                        a.getString(R.string.learn_f_later), a.getString(R.string.learn_f_history)}, new Dialogs.Index() {
                    @Override public void on(int i) { filter = i; build(); } });
            } }), wrap(8));
        col.addView(text(f[filter], 13, R.color.accent), wrap(4));

        List<Items.Item> all = Items.query(a, TYPE, null, "status ASC, title ASC", 500);
        boolean any = false;
        for (final Items.Item it : all) {
            Meta m = it.meta();
            if (filter == 1 && !m.flag("fav")) continue;
            if (filter == 2 && !m.flag("later")) continue;
            if (filter == 3 && !m.flag("opened")) continue;
            any = true;
            String cat = categoryLabel(it.status);
            String sub = cat + (m.flag("verify") ? "  ·  " + a.getString(R.string.learn_verify) : "")
                    + (m.flag("fav") ? "  ★" : "") + (m.flag("later") ? "  ⏱" : "");
            col.addView(navCard(it.title, sub, new View.OnClickListener() { @Override public void onClick(View v) { menu(it); } }), wrap(8));
        }
        if (!any) col.addView(text(a.getString(R.string.learn_none), 14, R.color.text_secondary), wrap(16));
        col.addView(button(R.string.learn_add, true, new View.OnClickListener() { @Override public void onClick(View v) { add(); } }), wrap(16));
    }

    private String categoryLabel(String key) {
        for (int i = 0; i < CATS.length; i++) if (CATS[i].equals(key)) return a.getString(CAT_LABEL[i]);
        return key;
    }

    private void menu(final Items.Item it) {
        final Meta m = it.meta();
        final boolean video = "video".equals(m.get("kind"));
        String[] labels = {a.getString(video ? R.string.learn_watch : R.string.learn_open_channel),
                a.getString(m.flag("fav") ? R.string.learn_unfav : R.string.learn_fav),
                a.getString(m.flag("later") ? R.string.learn_unlater : R.string.learn_later),
                a.getString(R.string.learn_note), a.getString(R.string.learn_remove)};
        Dialogs.choose(a, it.title, labels, new Dialogs.Index() {
            @Override public void on(int i) {
                if (i == 0) open(it, video);
                else if (i == 1) { Items.setMeta(a, it.id, m.setFlag("fav", !m.flag("fav"))); build(); }
                else if (i == 2) { Items.setMeta(a, it.id, m.setFlag("later", !m.flag("later"))); build(); }
                else if (i == 3) note(it, m);
                else { Items.delete(a, it.id); build(); }
            } });
    }

    private void open(Items.Item it, boolean video) {
        Items.setMeta(a, it.id, it.meta().setFlag("opened", true));
        if (video) { host.push(new PlayerScreen(a, host, it.title, it.body).view()); return; }
        try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(it.body))); }
        catch (ActivityNotFoundException e) { Toast.makeText(a, R.string.learn_no_app, Toast.LENGTH_LONG).show(); }
    }

    private void note(final Items.Item it, final Meta m) {
        Dialogs.prompt(a, R.string.learn_note, R.string.learn_note_hint, true, false, m.get("note", ""), new Dialogs.Text() {
            @Override public void on(String s) { Items.setMeta(a, it.id, m.set("note", s.replace(";", ",").replace("=", "-"))); build(); } });
    }

    private void add() {
        Dialogs.prompt(a, R.string.learn_add, R.string.learn_url_hint, false, false, null, new Dialogs.Text() {
            @Override public void on(final String url) {
                if (url.toLowerCase().contains("/shorts/")) { Toast.makeText(a, R.string.learn_no_shorts, Toast.LENGTH_LONG).show(); return; }
                final String id = YouTubeId.parse(url);
                if (id == null) { Toast.makeText(a, R.string.learn_bad_url, Toast.LENGTH_LONG).show(); return; }
                final String[] labels = new String[CATS.length];
                for (int i = 0; i < labels.length; i++) labels[i] = a.getString(CAT_LABEL[i]);
                Dialogs.prompt(a, R.string.learn_add, R.string.learn_title_hint, false, false, null, new Dialogs.Text() {
                    @Override public void on(final String title) {
                        Dialogs.choose(a, a.getString(R.string.learn_category), labels, new Dialogs.Index() {
                            @Override public void on(int i) {
                                Items.addFull(a, TYPE, title, id, CATS[i], 0, new Meta().set("kind", "video").toString());
                                build(); } });
                    } });
            } });
    }
}

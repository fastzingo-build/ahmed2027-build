package com.ahmed2027.app.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ahmed2027.app.R;
import com.ahmed2027.app.growth.GrowthScore;
import com.ahmed2027.app.growth.ScoreInputs;
import com.ahmed2027.app.prayer.PrayerNames;
import com.ahmed2027.app.prayer.PrayerProvider;

import java.text.DateFormat;
import java.util.Date;

/** Grow tab: weekly Growth Score (a trend, never a grade) and the six growth areas. */
public final class GrowScreen {
    private GrowScreen() {}

    private static TextView tv(Activity a, CharSequence s, int sp, int color) {
        TextView t = new TextView(a);
        t.setText(s); t.setTextSize(sp); t.setTextColor(a.getColor(color)); t.setLineSpacing(0, 1.25f);
        return t;
    }

    private static LinearLayout.LayoutParams lp(Activity a, int top) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        l.topMargin = Ui.dp(a, top);
        return l;
    }

    private static Card nav(final Activity a, int titleRes, CharSequence sub, View.OnClickListener l) {
        Card c = new Card(a);
        c.addView(tv(a, a.getString(titleRes), 20, R.color.text_primary));
        if (sub != null) { TextView s = tv(a, sub, 14, R.color.text_secondary); s.setPadding(0, Ui.dp(a, 6), 0, 0); c.addView(s); }
        c.setClickable(true);
        c.setContentDescription(a.getString(titleRes) + (sub == null ? "" : ", " + sub));
        c.setOnClickListener(l);
        return c;
    }

    private static String pct(Activity a, Integer v) { return v == null ? a.getString(R.string.score_none) : v + "%"; }

    public static View build(final Activity a, final Host host) {
        ScrollView sv = new ScrollView(a);
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 16);
        root.setPadding(p, Ui.dp(a, 24), p, p);
        sv.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title = tv(a, a.getString(R.string.title_grow), 24, R.color.text_primary);
        title.setAccessibilityHeading(true);
        root.addView(title);

        // Weekly Growth Score
        GrowthScore.Input now = ScoreInputs.week(a, 0), prev = ScoreInputs.week(a, 1);
        Integer score = GrowthScore.score(now);
        Integer trend = GrowthScore.trend(score, GrowthScore.score(prev));
        Card sc = new Card(a);
        sc.addView(tv(a, a.getString(R.string.score_title), 16, R.color.text_secondary));
        sc.addView(tv(a, score == null ? a.getString(R.string.score_not_enough) : a.getString(R.string.score_value, score), 28, R.color.text_primary));
        if (trend != null) sc.addView(tv(a, trend > 0 ? a.getString(R.string.score_up, trend) : trend < 0 ? a.getString(R.string.score_down, -trend)
                : a.getString(R.string.score_same), 14, R.color.accent));
        String comp = a.getString(R.string.score_components, pct(a, now.prayer), pct(a, now.habits), pct(a, now.learning),
                pct(a, now.movement), pct(a, now.creation), pct(a, now.focus));
        TextView ct = tv(a, comp, 13, R.color.text_secondary);
        ct.setPadding(0, Ui.dp(a, 8), 0, 0);
        sc.addView(ct);
        TextView note = tv(a, a.getString(R.string.score_note), 12, R.color.text_secondary);
        note.setPadding(0, Ui.dp(a, 8), 0, 0);
        sc.addView(note);
        root.addView(sc, lp(a, 16));

        PrayerProvider.Next n = PrayerProvider.next(a);
        String nextTxt = a.getString(R.string.grow_prayer_next, PrayerNames.label(a, n.prayer),
                DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(n.timeWall)));
        root.addView(nav(a, R.string.grow_prayer, nextTxt, new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new PrayerScreen(a, host).view()); } }), lp(a, 12));
        root.addView(nav(a, R.string.grow_habits, a.getString(R.string.grow_habits_sub), new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new HabitsScreen(a, host).view()); } }), lp(a, 12));
        root.addView(nav(a, R.string.grow_calm, a.getString(R.string.grow_calm_sub), new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new PsychologyScreen(a, host).view()); } }), lp(a, 12));
        root.addView(nav(a, R.string.grow_personal, a.getString(R.string.grow_personal_sub), new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new PersonalDevScreen(a, host).view()); } }), lp(a, 12));
        root.addView(nav(a, R.string.grow_learn, a.getString(R.string.grow_learn_sub), new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new LearnScreen(a, host).view()); } }), lp(a, 12));
        root.addView(nav(a, R.string.grow_fitness, a.getString(R.string.grow_fitness_sub), new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new FitnessScreen(a, host).view()); } }), lp(a, 12));
        return sv;
    }
}

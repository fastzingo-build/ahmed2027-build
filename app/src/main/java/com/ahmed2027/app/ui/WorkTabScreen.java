package com.ahmed2027.app.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.ActivityLog;

/** Work tab: Work, Creator, Focus & notifications, Analytics. */
public final class WorkTabScreen {
    private WorkTabScreen() {}

    private static void add(final Activity a, LinearLayout root, int titleRes, String sub, View.OnClickListener l) {
        Card c = new Card(a);
        TextView t = new TextView(a);
        t.setText(titleRes); t.setTextSize(20); t.setTextColor(a.getColor(R.color.text_primary));
        c.addView(t);
        TextView s = new TextView(a);
        s.setText(sub); s.setTextSize(14); s.setTextColor(a.getColor(R.color.text_secondary));
        s.setPadding(0, Ui.dp(a, 6), 0, 0);
        c.addView(s);
        c.setClickable(true);
        c.setContentDescription(a.getString(titleRes) + ", " + sub);
        c.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(a, 12);
        root.addView(c, lp);
    }

    public static View build(final Activity a, final Host host) {
        ScrollView sv = new ScrollView(a);
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 16);
        root.setPadding(p, Ui.dp(a, 24), p, p);
        sv.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title = new TextView(a);
        title.setText(R.string.title_work); title.setTextSize(24); title.setTextColor(a.getColor(R.color.text_primary));
        title.setAccessibilityHeading(true);
        root.addView(title);
        add(a, root, R.string.work_title, a.getString(R.string.work_tab_sub), new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new WorkScreen(a, host).view()); } });
        add(a, root, R.string.creator_title, a.getString(R.string.creator_tab_sub), new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new CreatorScreen(a, host).view()); } });
        add(a, root, R.string.focus_title, a.getString(R.string.focus_tab_sub, ActivityLog.lastDays(a, ActivityLog.FOCUS, 7)), new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new FocusScreen(a, host).view()); } });
        add(a, root, R.string.an_title, a.getString(R.string.an_tab_sub), new View.OnClickListener() {
            @Override public void onClick(View v) { host.push(new AnalyticsScreen(a, host).view()); } });
        return sv;
    }
}

package com.ahmed2027.app.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ahmed2027.app.R;

/** Scrollable sub-screen with a Back button and a title. Subclasses fill `col`. */
abstract class ScreenBase {
    protected final Activity a;
    protected final Host host;
    protected final ScrollView sv;
    protected final LinearLayout col;

    ScreenBase(Activity activity, Host host, int titleRes) {
        this.a = activity; this.host = host;
        sv = new ScrollView(a);
        sv.setFillViewport(true);
        col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 16);
        col.setPadding(p, Ui.dp(a, 16), p, p);
        sv.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // header
        PrimaryButton back = new PrimaryButton(a, a.getString(R.string.action_back), false);
        back.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { ScreenBase.this.host.pop(); } });
        col.addView(back, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView t = new TextView(a);
        t.setText(titleRes);
        t.setTextColor(a.getColor(R.color.text_primary));
        t.setTextSize(24);
        t.setPadding(0, Ui.dp(a, 12), 0, 0);
        t.setAccessibilityHeading(true);
        col.addView(t);
    }

    public View view() { return sv; }

    protected int headerCount() { return 2; }

    /** Removes everything below the header so the screen can be rebuilt. */
    protected void clearBody() {
        while (col.getChildCount() > headerCount()) col.removeViewAt(col.getChildCount() - 1);
    }

    protected TextView text(CharSequence s, int sp, int color) {
        TextView t = new TextView(a);
        t.setText(s); t.setTextSize(sp); t.setTextColor(a.getColor(color)); t.setLineSpacing(0, 1.25f);
        return t;
    }

    protected LinearLayout.LayoutParams wrap(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(a, topDp);
        return lp;
    }

    /** Standard tappable card with a title and an optional subtitle. */
    protected Card navCard(int titleRes, CharSequence subtitle, View.OnClickListener l) {
        return navCard(a.getString(titleRes), subtitle, l);
    }

    protected Card navCard(String title, CharSequence subtitle, View.OnClickListener l) {
        Card c = new Card(a);
        c.addView(text(title, 18, R.color.text_primary));
        if (subtitle != null && subtitle.length() > 0) {
            TextView s = text(subtitle, 14, R.color.text_secondary);
            s.setPadding(0, Ui.dp(a, 6), 0, 0);
            c.addView(s);
        }
        c.setClickable(true);
        c.setContentDescription(title + (subtitle == null ? "" : ", " + subtitle));
        c.setOnClickListener(l);
        return c;
    }

    protected void push(ScreenBase s) { host.push(s.view()); }

    protected PrimaryButton button(int labelRes, boolean filled, View.OnClickListener l) {
        PrimaryButton b = new PrimaryButton(a, a.getString(labelRes), filled);
        b.setOnClickListener(l);
        return b;
    }
}

package com.ahmed2027.app.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Prefs;

/** Dhikr counter (kept locally) and a short morning/evening list of well-known adhkar. No Quran text. */
public final class AdhkarScreen extends ScreenBase {
    private static final int[] TEXT = {R.string.adhkar_1, R.string.adhkar_2, R.string.adhkar_3, R.string.adhkar_4, R.string.adhkar_5};
    private static final int[] NOTE = {R.string.adhkar_1n, R.string.adhkar_2n, R.string.adhkar_3n, R.string.adhkar_4n, R.string.adhkar_5n};
    private static final int[] MORNING = {0, 1, 2, 3};
    private static final int[] EVENING = {0, 4, 2, 3};

    private final SharedPreferences prefs;

    public AdhkarScreen(Activity a, Host host) {
        super(a, host, R.string.pr_adhkar);
        prefs = Prefs.get(a);
        build();
    }

    private void build() {
        clearBody();
        col.addView(new SectionTitle(a, a.getString(R.string.dhikr_counter)));
        Card c = new Card(a);
        final TextView count = text(String.valueOf(prefs.getInt("dhikr_count", 0)), 56, R.color.accent);
        count.setGravity(Gravity.CENTER);
        count.setTypeface(Typeface.DEFAULT_BOLD);
        count.setContentDescription(a.getString(R.string.dhikr_counter));
        c.addView(count);
        PrimaryButton tap = new PrimaryButton(a, a.getString(R.string.dhikr_tap), true);
        tap.setMinHeight(Ui.dp(a, 72));
        tap.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            int n = prefs.getInt("dhikr_count", 0) + 1;
            prefs.edit().putInt("dhikr_count", n).apply();
            count.setText(String.valueOf(n));
        } });
        c.addView(tap, wrap(12));
        c.addView(button(R.string.dhikr_reset, false, new View.OnClickListener() { @Override public void onClick(View v) {
            prefs.edit().putInt("dhikr_count", 0).apply(); count.setText("0"); } }), wrap(8));
        col.addView(c, wrap(0));

        col.addView(new SectionTitle(a, a.getString(R.string.adhkar_morning)));
        col.addView(list(MORNING), wrap(0));
        col.addView(new SectionTitle(a, a.getString(R.string.adhkar_evening)));
        col.addView(list(EVENING), wrap(0));
        col.addView(text(a.getString(R.string.adhkar_disclaimer), 13, R.color.text_secondary), wrap(16));
    }

    private View list(int[] idx) {
        Card c = new Card(a);
        for (int i = 0; i < idx.length; i++) {
            TextView ar = text(a.getString(TEXT[idx[i]]), 20, R.color.text_primary);
            ar.setLineSpacing(0, 1.5f);
            ar.setPadding(0, i == 0 ? 0 : Ui.dp(a, 16), 0, 0);
            c.addView(ar);
            c.addView(text(a.getString(NOTE[idx[i]]), 13, R.color.text_secondary));
        }
        return c;
    }
}

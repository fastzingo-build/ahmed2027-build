package com.ahmed2027.app.ui;

import android.app.Activity;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Items;

import java.time.LocalDate;
import java.util.List;

/** Evening ritual (3 questions, ~2 minutes) or Friday weekly review. Saved locally only. */
public final class ReflectionScreen extends ScreenBase {
    private final boolean weekly;

    public ReflectionScreen(Activity a, Host host, boolean weekly) {
        super(a, host, weekly ? R.string.reflect_weekly : R.string.reflect_evening);
        this.weekly = weekly;
        build();
    }

    private void build() {
        clearBody();
        final String type = weekly ? "reflection_weekly" : "reflection";
        final String date = LocalDate.now(Clock.zone()).toString();
        int[] q = weekly ? new int[]{R.string.rw_q1, R.string.rw_q2, R.string.rw_q3} : new int[]{R.string.re_q1, R.string.re_q2, R.string.re_q3};
        col.addView(text(a.getString(R.string.reflect_intro), 14, R.color.text_secondary), wrap(12));
        final EditText[] f = new EditText[3];
        for (int i = 0; i < 3; i++) {
            col.addView(new SectionTitle(a, a.getString(q[i])));
            f[i] = new EditText(a);
            f[i].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            f[i].setMinLines(2);
            f[i].setTextColor(a.getColor(R.color.text_primary));
            f[i].setBackground(Ui.rounded(a, a.getColor(R.color.card), 16));
            f[i].setPadding(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), Ui.dp(a, 12));
            col.addView(f[i], wrap(0));
        }
        col.addView(button(R.string.action_save, true, new View.OnClickListener() {
            @Override public void onClick(View v) {
                String body = f[0].getText().toString().trim() + "\n---\n" + f[1].getText().toString().trim() + "\n---\n" + f[2].getText().toString().trim();
                if (body.replace("-", "").trim().isEmpty()) { Toast.makeText(a, R.string.reflect_empty, Toast.LENGTH_SHORT).show(); return; }
                Items.addFull(a, type, date, body, "saved", 0, null);
                Toast.makeText(a, R.string.reflect_saved, Toast.LENGTH_LONG).show();
                host.pop();
            } }), wrap(16));

        List<Items.Item> past = Items.query(a, type, null, "created_ts DESC", 5);
        if (!past.isEmpty()) {
            col.addView(new SectionTitle(a, a.getString(R.string.reflect_past)));
            Card c = new Card(a);
            for (Items.Item it : past) {
                c.addView(text(it.title, 13, R.color.accent));
                c.addView(text(it.body.replace("\n---\n", "\n• "), 14, R.color.text_primary));
                android.view.View gap = new android.view.View(a);
                c.addView(gap, new android.widget.LinearLayout.LayoutParams(1, Ui.dp(a, 12)));
            }
            col.addView(c, wrap(0));
        }
    }
}

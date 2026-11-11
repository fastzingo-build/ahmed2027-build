package com.ahmed2027.app.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmed2027.app.R;

/** First run: 4 cards (what it is, language, default launcher, permissions note). */
public final class OnboardingScreen {
    public static final int STEPS = 4;
    private static final int[] TITLES = {R.string.ob1_title, R.string.ob2_title, R.string.ob3_title, R.string.ob4_title};
    private static final int[] BODIES = {R.string.ob1_body, R.string.ob2_body, R.string.ob3_body, R.string.ob4_body};

    public interface StepHost { void setStep(int step); }

    private OnboardingScreen() {}

    public static View build(final Activity a, final Host host, final int step, final StepHost sh) {
        ScrollView sv = new ScrollView(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(a, 24);
        root.setPadding(p, p, p, p);
        sv.setFillViewport(true);
        sv.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView count = new TextView(a);
        count.setText(a.getString(R.string.ob_step, step + 1, STEPS));
        count.setTextColor(a.getColor(R.color.text_secondary));
        count.setTextSize(13);
        root.addView(count);

        Card card = new Card(a);
        TextView t = new TextView(a);
        t.setText(TITLES[step]);
        t.setTextColor(a.getColor(R.color.text_primary));
        t.setTextSize(24);
        t.setAccessibilityHeading(true);
        card.addView(t);
        TextView b = new TextView(a);
        b.setText(BODIES[step]);
        b.setTextColor(a.getColor(R.color.text_secondary));
        b.setTextSize(16);
        b.setLineSpacing(0, 1.3f);
        b.setPadding(0, Ui.dp(a, 12), 0, 0);
        card.addView(b);

        if (step == 1) card.addView(MoreScreen.languageCard(a), MoreScreen.buttonParams(a));
        if (step == 2) {
            PrimaryButton open = new PrimaryButton(a, a.getString(R.string.ob3_action), true);
            open.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { openHomeChooser(a); }
            });
            card.addView(open, MoreScreen.buttonParams(a));
        }
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cl.topMargin = Ui.dp(a, 8);
        root.addView(card, cl);

        boolean last = step == STEPS - 1;
        PrimaryButton next = new PrimaryButton(a, a.getString(last ? R.string.ob_start : R.string.ob_next), true);
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (step == STEPS - 1) host.finishOnboarding(); else sh.setStep(step + 1);
            }
        });
        root.addView(next, MoreScreen.buttonParams(a));
        if (step > 0) {
            PrimaryButton back = new PrimaryButton(a, a.getString(R.string.ob_back), false);
            back.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { sh.setStep(step - 1); }
            });
            root.addView(back, MoreScreen.buttonParams(a));
        }
        return sv;
    }

    /** Opens the system Home-app chooser; the user makes the choice themselves. */
    private static void openHomeChooser(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (ActivityNotFoundException e) {
            try {
                a.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(a, R.string.ob3_failed, Toast.LENGTH_LONG).show();
            }
        }
    }
}

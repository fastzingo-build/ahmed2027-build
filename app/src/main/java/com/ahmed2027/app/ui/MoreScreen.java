package com.ahmed2027.app.ui;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Lang;
import com.ahmed2027.app.core.LocaleManager;
import com.ahmed2027.app.core.Prefs;
import com.ahmed2027.app.friendkey.FriendKeyGate;
import com.ahmed2027.app.friendkey.FriendKeyStore;
import com.ahmed2027.app.friendkey.UnlockManager;
import com.ahmed2027.app.shield.ShieldStatus;

/** More tab: settings (language, text size, backup/restore, about). */
public final class MoreScreen {
    private static final int[] SIZE_LABELS = {R.string.size_small, R.string.size_normal, R.string.size_large, R.string.size_xlarge};

    private MoreScreen() {}

    public static View build(final Activity a, final Host host) {
        ScrollView sv = new ScrollView(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 16);
        root.setPadding(p, Ui.dp(a, 24), p, p);
        sv.addView(root);

        TextView title = new TextView(a);
        title.setText(R.string.title_more);
        title.setTextColor(a.getColor(R.color.text_primary));
        title.setTextSize(24);
        title.setAccessibilityHeading(true);
        root.addView(title);

        root.addView(new SectionTitle(a, a.getString(R.string.section_shield)));
        root.addView(shieldCard(a, host));

        root.addView(new SectionTitle(a, a.getString(R.string.section_friendkey)));
        root.addView(friendKeyCard(a));

        root.addView(new SectionTitle(a, a.getString(R.string.section_language)));
        root.addView(languageCard(a));

        root.addView(new SectionTitle(a, a.getString(R.string.section_text_size)));
        Card size = new Card(a);
        int cur = Prefs.getTextScaleIndex(a);
        for (int i = 0; i < SIZE_LABELS.length; i++) {
            final int idx = i;
            PrimaryButton b = new PrimaryButton(a, a.getString(SIZE_LABELS[i]), i == cur);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (idx == Prefs.getTextScaleIndex(a)) return;
                    Prefs.setTextScaleIndex(a, idx);
                    a.recreate();
                }
            });
            size.addView(b, buttonParams(a));
        }
        root.addView(size);

        root.addView(new SectionTitle(a, a.getString(R.string.section_backup)));
        Card backup = new Card(a);
        TextView note = new TextView(a);
        note.setText(R.string.backup_note);
        note.setTextColor(a.getColor(R.color.text_secondary));
        note.setTextSize(14);
        backup.addView(note);
        PrimaryButton exp = new PrimaryButton(a, a.getString(R.string.backup_export), true);
        exp.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { host.startExport(); } });
        backup.addView(exp, buttonParams(a));
        PrimaryButton imp = new PrimaryButton(a, a.getString(R.string.backup_import), false);
        imp.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { host.startImport(); } });
        backup.addView(imp, buttonParams(a));
        root.addView(backup);

        root.addView(new SectionTitle(a, a.getString(R.string.section_about)));
        Card about = new Card(a);
        TextView body = new TextView(a);
        body.setText(R.string.about_body);
        body.setTextColor(a.getColor(R.color.text_primary));
        body.setTextSize(15);
        body.setLineSpacing(0, 1.25f);
        about.addView(body);
        TextView ver = new TextView(a);
        ver.setText(a.getString(R.string.about_version, versionName(a)));
        ver.setTextColor(a.getColor(R.color.text_secondary));
        ver.setTextSize(13);
        ver.setPadding(0, Ui.dp(a, 8), 0, 0);
        about.addView(ver);
        root.addView(about);
        return sv;
    }

    /** Explanation first; Android's Accessibility screen opens only after the user taps the button. */
    private static View shieldCard(final Activity a, final Host host) {
        Card c = new Card(a);
        TextView ex = new TextView(a);
        ex.setText(R.string.shield_explain);
        ex.setTextColor(a.getColor(R.color.text_secondary));
        ex.setTextSize(14);
        ex.setLineSpacing(0, 1.25f);
        c.addView(ex);

        final boolean svc = ShieldStatus.serviceEnabled(a);
        final boolean on = ShieldStatus.isOn(a);
        TextView st = new TextView(a);
        st.setText(!svc ? R.string.shield_status_off : (on ? R.string.shield_status_on : R.string.shield_status_paused));
        st.setTextColor(a.getColor(svc && on ? R.color.accent : R.color.warning));
        st.setTextSize(15);
        st.setPadding(0, Ui.dp(a, 12), 0, 0);
        c.addView(st);

        PrimaryButton open = new PrimaryButton(a, a.getString(R.string.shield_open_settings), !svc);
        open.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { a.startActivity(new android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
                catch (android.content.ActivityNotFoundException e) {
                    android.widget.Toast.makeText(a, R.string.shield_open_failed, android.widget.Toast.LENGTH_LONG).show();
                }
            }
        });
        c.addView(open, buttonParams(a));

        PrimaryButton toggle = new PrimaryButton(a, a.getString(on ? R.string.shield_toggle_off : R.string.shield_toggle_on), false);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean turningOff = ShieldStatus.isOn(a);
                if (turningOff && !FriendKeyGate.active(a, UnlockManager.SETTINGS, null)) {
                    FriendKeyGate.request(a, UnlockManager.SETTINGS, null); // protected action
                    return;
                }
                ShieldStatus.setOn(a, !turningOff);
                a.recreate();
            }
        });
        c.addView(toggle, buttonParams(a));
        return c;
    }

    /** Friend Key: always reachable. Disabling the cooling-off wait is itself a protected action. */
    private static View friendKeyCard(final Activity a) {
        Card c = new Card(a);
        TextView ex = new TextView(a);
        ex.setText(R.string.fk_card_body);
        ex.setTextColor(a.getColor(R.color.text_secondary));
        ex.setTextSize(14);
        ex.setLineSpacing(0, 1.25f);
        c.addView(ex);

        long left = FriendKeyGate.remaining(a, UnlockManager.SETTINGS, null);
        if (left > 0) {
            TextView st = new TextView(a);
            st.setText(a.getString(R.string.fk_unlocked_for, (left + 59999) / 60000));
            st.setTextColor(a.getColor(R.color.accent));
            st.setTextSize(15);
            st.setPadding(0, Ui.dp(a, 12), 0, 0);
            c.addView(st);
        }

        PrimaryButton open = new PrimaryButton(a, a.getString(R.string.fk_open), true);
        open.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { FriendKeyGate.request(a, UnlockManager.SETTINGS, null); }
        });
        c.addView(open, buttonParams(a));

        final FriendKeyStore store = new FriendKeyStore(a);
        final boolean cooling = store.coolingEnabled();
        PrimaryButton cool = new PrimaryButton(a, a.getString(cooling ? R.string.fk_cool_disable : R.string.fk_cool_enable), false);
        cool.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (store.coolingEnabled()) {
                    if (!FriendKeyGate.active(a, UnlockManager.SETTINGS, null)) {
                        FriendKeyGate.request(a, UnlockManager.SETTINGS, null);
                        return;
                    }
                    store.setCoolingEnabled(false);
                } else {
                    store.setCoolingEnabled(true); // making things stricter is free
                }
                store.clearCooling();
                a.recreate();
            }
        });
        c.addView(cool, buttonParams(a));
        TextView cs = new TextView(a);
        cs.setText(cooling ? R.string.fk_cool_state_on : R.string.fk_cool_state_off);
        cs.setTextColor(a.getColor(R.color.text_secondary));
        cs.setTextSize(13);
        cs.setPadding(0, Ui.dp(a, 8), 0, 0);
        c.addView(cs);
        return c;
    }

    /** Shared by Settings and onboarding. Changing language recreates the activity. */
    public static View languageCard(final Activity a) {
        Card lang = new Card(a);
        TextView hint = new TextView(a);
        hint.setText(R.string.language_hint);
        hint.setTextColor(a.getColor(R.color.text_secondary));
        hint.setTextSize(14);
        lang.addView(hint);
        String current = LocaleManager.current(a);
        lang.addView(langButton(a, R.string.language_arabic, Lang.AR, Lang.AR.equals(current)), buttonParams(a));
        lang.addView(langButton(a, R.string.language_english, Lang.EN, Lang.EN.equals(current)), buttonParams(a));
        return lang;
    }

    private static View langButton(final Activity a, int label, final String code, boolean active) {
        PrimaryButton b = new PrimaryButton(a, a.getString(label), active);
        if (active) b.setContentDescription(a.getString(label) + ", " + a.getString(R.string.language_active));
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (code.equals(LocaleManager.current(a))) return;
                LocaleManager.setLanguage(a, code);
                a.recreate();
            }
        });
        return b;
    }

    static LinearLayout.LayoutParams buttonParams(Activity a) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(a, 12);
        return lp;
    }

    private static String versionName(Activity a) {
        try {
            String v = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
            return v == null ? "?" : v;
        } catch (PackageManager.NameNotFoundException e) { return "?"; }
    }
}

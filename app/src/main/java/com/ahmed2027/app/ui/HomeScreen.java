package com.ahmed2027.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.core.Clock;
import com.ahmed2027.app.core.Db;
import com.ahmed2027.app.core.Items;
import com.ahmed2027.app.core.Prefs;
import com.ahmed2027.app.launcher.AppClassStore;
import com.ahmed2027.app.prayer.PrayerNames;
import com.ahmed2027.app.prayer.PrayerProvider;
import com.ahmed2027.app.social.SocialSessionManager;
import com.ahmed2027.app.social.SqlSessionStore;

import java.text.DateFormat;
import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Today dashboard. */
public final class HomeScreen {
    private final Activity a;
    private final Host host;
    private final ScrollView sv;
    private final LinearLayout col;
    private CountDownTimer timer;

    public HomeScreen(Activity activity, Host host) {
        this.a = activity; this.host = host;
        sv = new ScrollView(a);
        col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 16);
        col.setPadding(p, Ui.dp(a, 24), p, p);
        sv.addView(col);

        // Swipe up = open the app drawer (the Apps tab). A visible button exists too.
        final GestureDetector gd = new GestureDetector(a, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 != null && e2 != null && vy < -1500 && e1.getY() - e2.getY() > Ui.dp(a, 120)
                        && !sv.canScrollVertically(1)) {
                    HomeScreen.this.host.goToTab(ShellScreens.APPS);
                }
                return false;
            }
        });
        sv.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) { gd.onTouchEvent(e); return false; }
        });
        build();
    }

    public View view() { return sv; }

    public void stop() { if (timer != null) timer.cancel(); }

    private String today() { return LocalDate.now(Clock.zone()).toString(); }

    private void build() {
        col.removeAllViews();
        Locale loc = Locale.getDefault();

        TextClock clock = new TextClock(a);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("h:mm");
        clock.setTextSize(64);
        clock.setTextColor(a.getColor(R.color.text_primary));
        clock.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
        col.addView(clock);

        TextView dates = text(DateFormat.getDateInstance(DateFormat.FULL, loc).format(new Date()), 15, R.color.text_primary);
        col.addView(dates);
        String hijri;
        try {
            hijri = DateTimeFormatter.ofPattern("d MMMM y G", loc).format(HijrahDate.from(LocalDate.now(Clock.zone())));
        } catch (RuntimeException e) { hijri = ""; }
        if (!hijri.isEmpty()) col.addView(text(hijri, 14, R.color.text_secondary));

        col.addView(prayerCard());
        col.addView(intentionCard());
        col.addView(tasksCard());
        col.addView(habitCard());
        col.addView(socialCard());

        PrimaryButton small = new PrimaryButton(a, a.getString(R.string.small_start), true);
        small.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showSmallStart(); }
        });
        LinearLayout.LayoutParams slp = lp(); slp.topMargin = Ui.dp(a, 16);
        col.addView(small, slp);

        col.addView(dock());
        PrimaryButton all = new PrimaryButton(a, a.getString(R.string.all_apps), false);
        all.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { host.goToTab(ShellScreens.APPS); }
        });
        LinearLayout.LayoutParams alp = lp(); alp.topMargin = Ui.dp(a, 8);
        col.addView(all, alp);
    }

    private LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView text(CharSequence t, int sp, int colorRes) {
        TextView v = new TextView(a);
        v.setText(t); v.setTextSize(sp); v.setTextColor(a.getColor(colorRes));
        v.setLineSpacing(0, 1.25f);
        return v;
    }

    private Card card(int titleRes) {
        Card c = new Card(a);
        TextView h = text(a.getString(titleRes), 13, R.color.text_secondary);
        h.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        h.setAccessibilityHeading(true);
        c.addView(h);
        LinearLayout.LayoutParams l = lp(); l.topMargin = Ui.dp(a, 16);
        c.setLayoutParams(l);
        return c;
    }

    // ---- prayer (hook for Phase 5)
    private View prayerCard() {
        Card c = card(R.string.card_next_prayer);
        PrayerProvider.Next n = PrayerProvider.next(a);
        long mins = Math.max(0, (n.timeWall - Clock.wall() + 59_999) / 60_000);
        TextView name = text(PrayerNames.label(a, n.prayer) + "  "
                + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(n.timeWall)), 22, R.color.accent);
        name.setPadding(0, Ui.dp(a, 8), 0, 0);
        c.addView(name);
        c.addView(text(a.getString(R.string.prayer_in_hm, mins / 60, mins % 60)
                + (n.estimated ? "  ·  " + a.getString(R.string.pr_src_estimated_short) : ""), 14, R.color.text_secondary));
        c.setClickable(true);
        c.setContentDescription(a.getString(R.string.card_next_prayer) + ": " + name.getText());
        c.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                host.goToTab(ShellScreens.GROW);
                host.push(new PrayerScreen(a, host).view());
            }
        });
        return c;
    }

    // ---- daily intention
    private View intentionCard() {
        Card c = card(R.string.card_intention);
        final String d = today();
        String cur = Prefs.getIntention(a, d);
        TextView t = text(cur.isEmpty() ? a.getString(R.string.intention_empty) : cur, 18,
                cur.isEmpty() ? R.color.text_secondary : R.color.text_primary);
        t.setPadding(0, Ui.dp(a, 8), 0, 0);
        t.setMinHeight(Ui.dp(a, 48));
        t.setGravity(Gravity.CENTER_VERTICAL);
        c.addView(t);
        c.setClickable(true);
        c.setContentDescription(a.getString(R.string.card_intention) + ": "
                + (cur.isEmpty() ? a.getString(R.string.intention_empty) : cur));
        c.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                final EditText et = new EditText(a);
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                et.setSingleLine(true);
                et.setText(Prefs.getIntention(a, d));
                new AlertDialog.Builder(a).setTitle(R.string.card_intention).setView(et)
                        .setPositiveButton(R.string.action_save, new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface di, int w) {
                                Prefs.setIntention(a, d, et.getText().toString().trim());
                                build();
                            }
                        }).setNegativeButton(R.string.action_cancel, null).show();
            }
        });
        return c;
    }

    // ---- 3 focus tasks
    private View tasksCard() {
        Card c = card(R.string.card_focus_tasks);
        List<Items.Item> tasks = Items.list(a, "task", "open", 3);
        if (tasks.isEmpty()) {
            TextView t = text(a.getString(R.string.tasks_empty), 15, R.color.text_secondary);
            t.setPadding(0, Ui.dp(a, 8), 0, 0);
            c.addView(t);
        }
        for (final Items.Item it : tasks) {
            CheckBox cb = new CheckBox(a);
            cb.setText(it.title);
            cb.setTextColor(a.getColor(R.color.text_primary));
            cb.setTextSize(16);
            cb.setMinHeight(Ui.dp(a, 48));
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(a.getColor(R.color.accent)));
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton b, boolean on) {
                    if (on) { Items.setStatus(a, it.id, "done"); build(); }
                }
            });
            c.addView(cb);
        }
        if (tasks.size() < 3) {
            PrimaryButton add = new PrimaryButton(a, a.getString(R.string.task_add), false);
            add.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { addTaskDialog(); }
            });
            LinearLayout.LayoutParams l = lp(); l.topMargin = Ui.dp(a, 8);
            c.addView(add, l);
        }
        return c;
    }

    private void addTaskDialog() {
        final EditText et = new EditText(a);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setSingleLine(true);
        new AlertDialog.Builder(a).setTitle(R.string.task_add).setView(et)
                .setPositiveButton(R.string.action_save, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface di, int w) {
                        String t = et.getText().toString().trim();
                        if (t.isEmpty()) { Toast.makeText(a, R.string.task_empty_title, Toast.LENGTH_SHORT).show(); return; }
                        Items.add(a, "task", t, "open");
                        build();
                    }
                }).setNegativeButton(R.string.action_cancel, null).show();
    }

    // ---- habit ring (habits are created in Phase 6; counts are real)
    private View habitCard() {
        Card c = card(R.string.card_habits);
        int total = Items.count(a, "habit", "active");
        int done = 0;
        Cursor cur = Db.get(a).getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM habit_log WHERE local_date=? AND done=1", new String[]{today()});
        try { if (cur.moveToFirst()) done = cur.getInt(0); } finally { cur.close(); }
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        HabitRing ring = new HabitRing(a);
        ring.set(done, total);
        ring.setContentDescription(a.getString(R.string.habits_progress, done, total));
        row.addView(ring);
        TextView t = text(total == 0 ? a.getString(R.string.habits_empty) : a.getString(R.string.habits_progress, done, total),
                15, R.color.text_primary);
        t.setPadding(Ui.dp(a, 16), 0, 0, 0);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams l = lp(); l.topMargin = Ui.dp(a, 8);
        c.addView(row, l);
        return c;
    }

    // ---- social time chips (read-only view of the quota table; sessions start in Phase 2)
    private View socialCard() {
        Card c = card(R.string.card_social);
        LinearLayout row = new LinearLayout(a);
        row.addView(chip(R.string.social_tiktok, SocialSessionManager.KEY_TIKTOK), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        View gap = new View(a);
        row.addView(gap, new LinearLayout.LayoutParams(Ui.dp(a, 8), 1));
        row.addView(chip(R.string.social_instagram, SocialSessionManager.KEY_INSTAGRAM), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams l = lp(); l.topMargin = Ui.dp(a, 8);
        c.addView(row, l);
        return c;
    }

    private View chip(int nameRes, String key) {
        SocialSessionManager m = SqlSessionStore.manager(a);
        long now = Clock.wall(), el = Clock.elapsed();
        SocialSessionManager.State st = m.state(key, now, el);
        String status;
        if (st == SocialSessionManager.State.ELIGIBLE) status = a.getString(R.string.social_available);
        else if (st == SocialSessionManager.State.ACTIVE) status = a.getString(R.string.social_left, (m.remainingMillis(key, now, el) + 59999) / 60000);
        else status = a.getString(R.string.social_used);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(Ui.rounded(a, a.getColor(R.color.divider), 16));
        int p = Ui.dp(a, 12);
        box.setPadding(p, p, p, p);
        box.addView(text(a.getString(nameRes), 14, R.color.text_primary));
        box.addView(text(status, 13, R.color.text_secondary));
        box.setContentDescription(a.getString(nameRes) + ", " + status);
        return box;
    }

    // ---- 2-minute small start
    private void showSmallStart() {
        String[] acts = a.getResources().getStringArray(R.array.small_start_actions);
        String action = acts[new Random().nextInt(acts.length)];
        final AlertDialog dlg = new AlertDialog.Builder(a)
                .setTitle(R.string.small_start)
                .setMessage(action + "\n\n02:00")
                .setPositiveButton(R.string.action_done, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        final String act = action;
        dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface d) { stop(); }
        });
        dlg.show();
        stop();
        timer = new CountDownTimer(120_000, 1000) {
            @Override public void onTick(long ms) {
                long s = (ms + 999) / 1000;
                dlg.setMessage(act + "\n\n" + String.format(Locale.US, "%02d:%02d", s / 60, s % 60));
            }
            @Override public void onFinish() { dlg.setMessage(act + "\n\n" + a.getString(R.string.small_start_done)); }
        }.start();
    }

    // ---- dock (up to 4 favorites)
    private View dock() {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rl = lp(); rl.topMargin = Ui.dp(a, 24);
        row.setLayoutParams(rl);
        Map<String, AppClassStore.Entry> m = AppClassStore.all(a);
        PackageManager pm = a.getPackageManager();
        int n = 0;
        for (Map.Entry<String, AppClassStore.Entry> e : m.entrySet()) {
            if (!e.getValue().favorite || n >= 4) continue;
            final String pkg = e.getKey();
            Intent li = pm.getLaunchIntentForPackage(pkg);
            if (li == null) continue; // uninstalled
            Drawable ic; CharSequence label;
            try { ic = pm.getApplicationIcon(pkg); label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)); }
            catch (PackageManager.NameNotFoundException ex) { continue; }
            ImageView iv = new ImageView(a);
            iv.setImageDrawable(ic);
            iv.setContentDescription(label);
            iv.setPadding(Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 8));
            iv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Intent i = a.getPackageManager().getLaunchIntentForPackage(pkg);
                    if (i != null) a.startActivity(i);
                }
            });
            row.addView(iv, new LinearLayout.LayoutParams(0, Ui.dp(a, 64), 1f));
            n++;
        }
        if (n == 0) {
            TextView t = text(a.getString(R.string.dock_empty), 13, R.color.text_secondary);
            t.setGravity(Gravity.CENTER);
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        return row;
    }
}

package com.ahmed2027.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ahmed2027.app.core.Backup;
import com.ahmed2027.app.core.LocaleManager;
import com.ahmed2027.app.core.Prefs;
import com.ahmed2027.app.friendkey.FriendKeyGate;
import com.ahmed2027.app.friendkey.UnlockManager;
import com.ahmed2027.app.prayer.PrayerBoot;
import com.ahmed2027.app.ui.BottomNav;
import com.ahmed2027.app.ui.Host;
import com.ahmed2027.app.ui.OnboardingScreen;
import com.ahmed2027.app.ui.ShellScreens;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shell: onboarding on first run, then content + 5-item bottom navigation. */
public class MainActivity extends Activity implements Host {
    private static final String STATE_TAB = "tab";
    private static final String STATE_STEP = "step";
    private static final int REQ_EXPORT = 101, REQ_IMPORT = 102;
    private static final int MAX_BACKUP_BYTES = 8 * 1024 * 1024;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private FrameLayout content;
    private BottomNav nav;
    private ShellScreens.Disposable current;
    private int step;
    private boolean resumedOnce;
    private final java.util.ArrayList<View> stack = new java.util.ArrayList<>();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.wrap(base));
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        int tab = ShellScreens.HOME;
        if (saved != null) { tab = saved.getInt(STATE_TAB, tab); step = saved.getInt(STATE_STEP, 0); }

        if (!Prefs.isOnboarded(this)) { showOnboarding(); return; }
        buildShell(tab);
    }

    // ---------- onboarding
    private void showOnboarding() {
        FrameLayout f = new FrameLayout(this);
        f.setBackgroundColor(getColor(R.color.bg));
        f.setFitsSystemWindows(true);
        f.addView(OnboardingScreen.build(this, this, step, new OnboardingScreen.StepHost() {
            @Override public void setStep(int s) { step = s; showOnboarding(); }
        }));
        setContentView(f);
    }

    @Override public void finishOnboarding() {
        Prefs.setOnboarded(this);
        buildShell(ShellScreens.HOME);
    }

    // ---------- shell
    private void buildShell(int tab) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.bg));
        root.setFitsSystemWindows(true);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        nav = new BottomNav(this);
        root.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);

        nav.setListener(new BottomNav.Listener() {
            @Override public void onSelected(int index) { show(index); }
        });
        nav.select(tab, false);
        show(tab);
        PrayerBoot.onAppStart(this);
        com.ahmed2027.app.focus.QuietAlarms.apply(this); // re-evaluate quiet periods and schedule the next boundary
    }

    @Override public void goToTab(int tab) { if (nav != null) nav.select(tab, true); }

    private void show(int tab) {
        if (current != null) { current.dispose(); current = null; }
        content.removeAllViews();
        stack.clear();
        ShellScreens.Built b = ShellScreens.build(this, this, tab);
        current = b.disposable;
        stack.add(b.view);
        content.addView(b.view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        b.view.setAlpha(0f);
        b.view.animate().alpha(1f).setDuration(180).start(); // 150-250 ms fade
    }

    @Override public void push(View screen) {
        content.removeAllViews();
        stack.add(screen);
        content.addView(screen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        screen.setAlpha(0f);
        screen.animate().alpha(1f).setDuration(180).start();
    }

    @Override public void pop() {
        if (stack.size() <= 1) return;
        stack.remove(stack.size() - 1);
        View prev = stack.get(stack.size() - 1);
        content.removeAllViews();
        prev.setAlpha(1f);
        content.addView(prev, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override public void onBackPressed() {
        if (stack.size() > 1) pop(); else super.onBackPressed();
    }

    @Override protected void onResume() {
        super.onResume();
        // Refresh dashboards (date, tasks, social chips) when coming back; skip first resume.
        if (resumedOnce && nav != null && nav.getSelected() == ShellScreens.HOME && stack.size() <= 1) show(ShellScreens.HOME);
        resumedOnce = true;
    }

    @Override protected void onDestroy() {
        if (current != null) current.dispose();
        super.onDestroy();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(STATE_TAB, nav == null ? ShellScreens.HOME : nav.getSelected());
        out.putInt(STATE_STEP, step);
    }

    // ---------- backup / restore via Storage Access Framework
    @Override public void startExport() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "ahmed2027-backup-"
                + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()) + ".json");
        startActivityForResult(i, REQ_EXPORT);
    }

    @Override public void startImport() {
        // Restoring a backup can change protections (app classes): needs the Friend Key.
        if (!FriendKeyGate.active(this, UnlockManager.SETTINGS, null)) {
            FriendKeyGate.request(this, UnlockManager.SETTINGS, null);
            return;
        }
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == FriendKeyGate.REQ_KEY) {
            if (res == RESULT_OK) {
                Toast.makeText(this, R.string.fk_unlocked_retry, Toast.LENGTH_LONG).show();
                if (nav != null) show(nav.getSelected());
            }
            return;
        }
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        if (req == REQ_EXPORT) doExport(uri);
        else if (req == REQ_IMPORT) doImport(uri);
    }

    private void toast(final int resId) {
        runOnUiThread(new Runnable() { @Override public void run() {
            Toast.makeText(MainActivity.this, resId, Toast.LENGTH_LONG).show(); } });
    }

    private void doExport(final Uri uri) {
        final Context app = getApplicationContext();
        io.execute(new Runnable() { @Override public void run() {
            try {
                byte[] bytes = Backup.export(app).getBytes(StandardCharsets.UTF_8);
                OutputStream os = getContentResolver().openOutputStream(uri, "wt");
                if (os == null) throw new java.io.IOException("no stream");
                try { os.write(bytes); } finally { os.close(); }
                toast(R.string.backup_export_ok);
            } catch (Exception e) { toast(R.string.backup_export_fail); }
        } });
    }

    private void doImport(final Uri uri) {
        final Context app = getApplicationContext();
        io.execute(new Runnable() { @Override public void run() {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) throw new java.io.IOException("no stream");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    byte[] buf = new byte[8192]; int n;
                    while ((n = is.read(buf)) > 0) {
                        bos.write(buf, 0, n);
                        if (bos.size() > MAX_BACKUP_BYTES) throw new java.io.IOException("too large");
                    }
                } finally { is.close(); }
                Backup.restore(app, new String(bos.toByteArray(), StandardCharsets.UTF_8));
                toast(R.string.backup_import_ok);
                runOnUiThread(new Runnable() { @Override public void run() { recreate(); } });
            } catch (Exception e) { toast(R.string.backup_import_fail); }
        } });
    }
}

package com.ahmed2027.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmed2027.app.R;
import com.ahmed2027.app.friendkey.FriendKeyGate;
import com.ahmed2027.app.friendkey.Protection;
import com.ahmed2027.app.friendkey.UnlockManager;
import com.ahmed2027.app.launcher.AppClass;
import com.ahmed2027.app.launcher.AppClassStore;
import com.ahmed2027.app.launcher.AppFilter;
import com.ahmed2027.app.launcher.AppInfo;
import com.ahmed2027.app.launcher.AppLoader;
import com.ahmed2027.app.shield.ShieldStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Apps tab: search + category chips + drawer grid. Long-press classifies / favorites. */
public final class AppsScreen {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    // chip order: All, Allowed, Limited, Blocked, Hidden
    private static final String[] CHIP_CLASS = {null, AppClass.ALLOWED, AppClass.LIMITED, AppClass.BLOCKED, AppClass.HIDDEN};
    private static final int[] CHIP_LABEL = {R.string.chip_all, R.string.class_allowed, R.string.class_limited,
            R.string.class_blocked, R.string.class_hidden};
    private static final int[] CLASS_LABEL = {R.string.class_allowed, R.string.class_limited,
            R.string.class_blocked, R.string.class_hidden};

    private final Activity a;
    private final FrameLayout body;       // swaps loading / error / empty / grid
    private final GridView grid;
    private final LinearLayout root;
    private final TextView[] chips = new TextView[CHIP_CLASS.length];
    private final EditText search;
    private List<AppInfo> apps = new ArrayList<>();
    private Map<String, AppClassStore.Entry> classes;
    private final List<AppInfo> shown = new ArrayList<>();
    private int chip = 0;
    private final Adapter adapter = new Adapter();

    public AppsScreen(Activity activity) {
        this.a = activity;
        root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 16);
        root.setPadding(p, Ui.dp(a, 24), p, 0);

        TextView title = new TextView(a);
        title.setText(R.string.title_apps);
        title.setTextColor(a.getColor(R.color.text_primary));
        title.setTextSize(24);
        title.setAccessibilityHeading(true);
        root.addView(title);

        if (!ShieldStatus.serviceEnabled(a) || !ShieldStatus.isOn(a)) {
            TextView banner = new TextView(a);
            banner.setText(R.string.shield_banner);
            banner.setTextColor(a.getColor(R.color.warning));
            banner.setTextSize(13);
            banner.setPadding(0, Ui.dp(a, 8), 0, 0);
            root.addView(banner);
        }

        search = new EditText(a);
        search.setHint(R.string.apps_search_hint);
        search.setSingleLine(true);
        search.setTextColor(a.getColor(R.color.text_primary));
        search.setHintTextColor(a.getColor(R.color.text_secondary));
        search.setBackground(Ui.rounded(a, a.getColor(R.color.card), 16));
        search.setPadding(Ui.dp(a, 16), 0, Ui.dp(a, 16), 0);
        search.setMinHeight(Ui.dp(a, 48));
        search.setContentDescription(a.getString(R.string.apps_search_hint));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 48));
        slp.topMargin = Ui.dp(a, 12);
        root.addView(search, slp);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refilter(); }
        });

        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < chips.length; i++) {
            final int idx = i;
            TextView t = new TextView(a);
            t.setText(CHIP_LABEL[i]);
            t.setTextSize(14);
            t.setGravity(Gravity.CENTER);
            t.setMinHeight(Ui.dp(a, 48));
            t.setPadding(Ui.dp(a, 16), 0, Ui.dp(a, 16), 0);
            t.setClickable(true);
            t.setFocusable(true);
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { chip = idx; styleChips(); refilter(); }
            });
            chips[i] = t;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(Ui.dp(a, 8));
            row.addView(t, lp);
        }
        hs.addView(row);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(a, 8);
        root.addView(hs, hlp);
        styleChips();

        body = new FrameLayout(a);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        grid = new GridView(a);
        grid.setNumColumns(4);
        grid.setVerticalSpacing(Ui.dp(a, 8));
        grid.setSelector(android.R.color.transparent);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) { launch(shown.get(pos)); }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                showMenu(shown.get(pos)); return true;
            }
        });

        load();
    }

    public View view() { return root; }

    private void styleChips() {
        for (int i = 0; i < chips.length; i++) {
            boolean on = i == chip;
            chips[i].setBackground(Ui.rounded(a, a.getColor(on ? R.color.accent : R.color.card), 24));
            chips[i].setTextColor(a.getColor(on ? R.color.on_accent : R.color.text_primary));
            chips[i].setSelected(on);
        }
    }

    private void setBody(View v) {
        body.removeAllViews();
        body.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void load() {
        setBody(new LoadingState(a));
        IO.execute(new Runnable() {
            @Override public void run() {
                List<AppInfo> loaded = null;
                Map<String, AppClassStore.Entry> cls = null;
                Exception err = null;
                try {
                    loaded = AppLoader.load(a);
                    cls = AppClassStore.all(a);
                } catch (Exception e) { err = e; }
                final List<AppInfo> fl = loaded; final Map<String, AppClassStore.Entry> fc = cls; final Exception fe = err;
                a.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (a.isDestroyed() || a.isFinishing()) return;
                        if (fe != null) {
                            setBody(new ErrorState(a, fe.getMessage(), new View.OnClickListener() {
                                @Override public void onClick(View v) { load(); }
                            }));
                            return;
                        }
                        apps = fl; classes = fc;
                        refilter();
                    }
                });
            }
        });
    }

    private String classOf(String pkg) {
        AppClassStore.Entry e = classes == null ? null : classes.get(pkg);
        return e == null ? AppClass.ALLOWED : e.cls;
    }

    private boolean isFav(String pkg) {
        AppClassStore.Entry e = classes == null ? null : classes.get(pkg);
        return e != null && e.favorite;
    }

    private void refilter() {
        shown.clear();
        String q = search.getText().toString();
        for (AppInfo ai : apps) {
            if (AppFilter.matches(ai.label, ai.pkg, classOf(ai.pkg), CHIP_CLASS[chip], q)) shown.add(ai);
        }
        if (shown.isEmpty()) {
            setBody(new EmptyState(a, a.getString(R.string.apps_empty_title), a.getString(R.string.apps_empty_body)));
        } else {
            adapter.notifyDataSetChanged();
            if (grid.getParent() == null) setBody(grid);
        }
    }

    private void launch(AppInfo ai) {
        Intent i = a.getPackageManager().getLaunchIntentForPackage(ai.pkg);
        if (i == null) { Toast.makeText(a, R.string.apps_cannot_open, Toast.LENGTH_SHORT).show(); return; }
        try { a.startActivity(i); }
        catch (Exception e) { Toast.makeText(a, R.string.apps_cannot_open, Toast.LENGTH_SHORT).show(); }
    }

    private void showMenu(final AppInfo ai) {
        String cur = classOf(ai.pkg);
        final boolean fav = isFav(ai.pkg);
        String[] items = new String[CLASS_LABEL.length + 1];
        for (int i = 0; i < CLASS_LABEL.length; i++) {
            items[i] = (AppClass.ALL[i].equals(cur) ? "✓ " : "") + a.getString(CLASS_LABEL[i]);
        }
        items[items.length - 1] = a.getString(fav ? R.string.menu_unfavorite : R.string.menu_favorite);
        new AlertDialog.Builder(a)
                .setTitle(ai.label)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which < AppClass.ALL.length) {
                            String newCls = AppClass.ALL[which];
                            if (Protection.classChangeNeedsKey(classOf(ai.pkg), newCls)
                                    && !FriendKeyGate.active(a, UnlockManager.CLASSIFY, null)) {
                                FriendKeyGate.request(a, UnlockManager.CLASSIFY, null); // then choose again
                                return;
                            }
                            AppClassStore.setClass(a, ai.pkg, newCls);
                        } else {
                            if (!fav && countFavorites() >= 4) {
                                Toast.makeText(a, R.string.dock_full, Toast.LENGTH_LONG).show();
                                return;
                            }
                            AppClassStore.setFavorite(a, ai.pkg, !fav);
                        }
                        classes = AppClassStore.all(a);
                        refilter();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private int countFavorites() {
        int n = 0;
        if (classes != null) for (AppClassStore.Entry e : classes.values()) if (e.favorite) n++;
        return n;
    }

    private final class Adapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout cell; ImageView icon; TextView label, tag;
            if (convert == null) {
                cell = new LinearLayout(a);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 8));
                icon = new ImageView(a);
                icon.setId(1);
                cell.addView(icon, new LinearLayout.LayoutParams(Ui.dp(a, 48), Ui.dp(a, 48)));
                label = new TextView(a);
                label.setId(2);
                label.setTextColor(a.getColor(R.color.text_primary));
                label.setTextSize(12);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                cell.addView(label);
                tag = new TextView(a);
                tag.setId(3);
                tag.setTextSize(10);
                tag.setTypeface(Typeface.DEFAULT_BOLD);
                cell.addView(tag);
            } else {
                cell = (LinearLayout) convert;
                icon = cell.findViewById(1); label = cell.findViewById(2); tag = cell.findViewById(3);
            }
            AppInfo ai = shown.get(pos);
            String cls = classOf(ai.pkg);
            icon.setImageDrawable(ai.icon);
            label.setText(ai.label);
            if (AppClass.ALLOWED.equals(cls)) {
                tag.setText("");
            } else {
                int idx = java.util.Arrays.asList(AppClass.ALL).indexOf(cls);
                tag.setText(CLASS_LABEL[idx]);
                tag.setTextColor(a.getColor(AppClass.BLOCKED.equals(cls) || AppClass.LIMITED.equals(cls)
                        ? R.color.warning : R.color.text_secondary));
            }
            cell.setContentDescription(ai.label + (AppClass.ALLOWED.equals(cls) ? "" : ", "
                    + a.getString(CLASS_LABEL[java.util.Arrays.asList(AppClass.ALL).indexOf(cls)])));
            return cell;
        }
    }
}

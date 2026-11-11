package com.ahmed2027.app.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ahmed2027.app.R;

/** Tab content dispatcher. Work stays "pending" until Phase 7; Grow has Prayer (Phase 5), more in Phase 6. */
public final class ShellScreens {
    public static final int HOME = 0, APPS = 1, GROW = 2, WORK = 3, MORE = 4;

    private ShellScreens() {}

    /** Optional lifecycle hook: Home needs to cancel its timer when replaced. */
    public interface Disposable { void dispose(); }

    public static final class Built {
        public final View view; public final Disposable disposable;
        Built(View v, Disposable d) { view = v; disposable = d; }
    }

    public static Built build(Activity a, Host host, int tab) {
        switch (tab) {
            case MORE: return new Built(MoreScreen.build(a, host), null);
            case APPS: return new Built(new AppsScreen(a).view(), null);
            case GROW: return new Built(GrowScreen.build(a, host), null);
            case WORK: return new Built(WorkTabScreen.build(a, host), null);
            default: {
                final HomeScreen h = new HomeScreen(a, host);
                return new Built(h.view(), new Disposable() { @Override public void dispose() { h.stop(); } });
            }
        }
    }

    private static View pending(Activity a, int titleRes) {
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 16);
        root.setPadding(p, Ui.dp(a, 24), p, p);
        TextView t = new TextView(a);
        t.setText(titleRes);
        t.setTextColor(a.getColor(R.color.text_primary));
        t.setTextSize(24);
        t.setAccessibilityHeading(true);
        root.addView(t);
        root.addView(new EmptyState(a, a.getString(R.string.pending_title), a.getString(R.string.pending_body)),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }
}

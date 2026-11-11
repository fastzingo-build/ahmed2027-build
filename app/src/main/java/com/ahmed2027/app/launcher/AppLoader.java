package com.ahmed2027.app.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Lists launchable apps (needs the <queries> entry). Call from a background thread. */
public final class AppLoader {
    private AppLoader() {}

    public static List<AppInfo> load(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(i, 0);
        List<AppInfo> out = new ArrayList<>(ris.size());
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (ResolveInfo ri : ris) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(c.getPackageName()) || !seen.add(pkg)) continue;
            CharSequence label = ri.loadLabel(pm);
            out.add(new AppInfo(label == null ? pkg : label.toString(), pkg, ri.loadIcon(pm)));
        }
        final Collator col = Collator.getInstance(Locale.getDefault());
        Collections.sort(out, new Comparator<AppInfo>() {
            @Override public int compare(AppInfo a, AppInfo b) { return col.compare(a.label, b.label); }
        });
        return out;
    }
}

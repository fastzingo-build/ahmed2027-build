package com.ahmed2027.app.launcher;

import android.graphics.drawable.Drawable;

public final class AppInfo {
    public final String label;
    public final String pkg;
    public final Drawable icon;
    public AppInfo(String label, String pkg, Drawable icon) { this.label = label; this.pkg = pkg; this.icon = icon; }
}

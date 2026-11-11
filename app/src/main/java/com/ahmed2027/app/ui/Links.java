package com.ahmed2027.app.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.ahmed2027.app.R;

/** Opens links and apps through Intents only. Only http(s) URLs are accepted. */
final class Links {
    private Links() {}

    static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || t.contains(" ")) return null;
        if (!t.startsWith("http://") && !t.startsWith("https://")) t = "https://" + t;
        Uri u = Uri.parse(t);
        return u.getHost() == null ? null : t;
    }

    static void openUrl(Activity a, String url) {
        String u = normalize(url);
        if (u == null) { Toast.makeText(a, R.string.link_bad, Toast.LENGTH_LONG).show(); return; }
        try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); }
        catch (ActivityNotFoundException e) { Toast.makeText(a, R.string.learn_no_app, Toast.LENGTH_LONG).show(); }
    }

    /** Installed app if there is one, otherwise the website. */
    static void openAppOrWeb(Activity a, String pkg, String fallbackUrl) {
        Intent i = a.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) { try { a.startActivity(i); return; } catch (ActivityNotFoundException ignored) { } }
        openUrl(a, fallbackUrl);
    }

    /** App only, never a website: social apps must go through the Shield's session rules, a browser would bypass them. */
    static void openAppOnly(Activity a, String pkg) {
        Intent i = a.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) { Toast.makeText(a, R.string.creator_app_missing, Toast.LENGTH_LONG).show(); return; }
        try { a.startActivity(i); } catch (ActivityNotFoundException e) { Toast.makeText(a, R.string.creator_app_missing, Toast.LENGTH_LONG).show(); }
    }
}

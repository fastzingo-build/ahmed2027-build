package com.ahmed2027.app.core;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Arabic (default) / English switch. Call wrap() from Activity.attachBaseContext
 * and recreate() the activity after setLanguage().
 */
public final class LocaleManager {
    private LocaleManager() {}

    public static Context wrap(Context base) {
        Locale locale = new Locale(Prefs.getLang(base));
        Locale.setDefault(locale);
        Configuration conf = new Configuration(base.getResources().getConfiguration());
        conf.setLocale(locale);
        conf.setLayoutDirection(locale); // RTL for ar, LTR for en
        conf.fontScale = Prefs.TEXT_SCALES[Prefs.getTextScaleIndex(base)];
        return base.createConfigurationContext(conf);
    }

    public static void setLanguage(Context c, String code) {
        Prefs.setLang(c, code);
    }

    public static String current(Context c) {
        return Prefs.getLang(c);
    }
}

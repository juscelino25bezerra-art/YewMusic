package com.newtube.mobile;

import android.content.Context;
import android.content.SharedPreferences;

import com.liskovsoft.sharedutils.prefs.GlobalPreferences;

import java.util.Locale;

/**
 * YEWPLAY(pt-BR): defaults that follow Brazilian conventions.
 *
 * <p>The shared {@code DateHelper} treats every "pt" locale as a 12-hour one, so in pt-BR the app
 * showed "2:30 PM". This turns the existing 24-hour setting (Settings -> Appearance and language)
 * on once, the first time the app starts in pt-BR. It never runs again, so a user who later turns
 * the setting off keeps it off. The shared module (a submodule fork) is left untouched.
 */
final class BrazilianDefaults {
    private static final String PREFS = "yewmusic_locale_defaults";
    private static final String KEY_24H_APPLIED = "ptbr_24h_applied";

    private BrazilianDefaults() {
    }

    static void apply(Context context) {
        try {
            if (!isBrazilianPortuguese(Locale.getDefault())) {
                return;
            }
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (prefs.getBoolean(KEY_24H_APPLIED, false)) {
                return;
            }
            GlobalPreferences.instance(context).set24HourLocaleEnabled(true);
            prefs.edit().putBoolean(KEY_24H_APPLIED, true).apply();
        } catch (RuntimeException e) {
            // A default is never worth a crash at startup.
            android.util.Log.w("YewMusic", "pt-BR defaults skipped", e);
        }
    }

    static boolean isBrazilianPortuguese(Locale locale) {
        return locale != null && "pt".equals(locale.getLanguage()) && "BR".equalsIgnoreCase(locale.getCountry());
    }
}

package com.android.launcher3.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class PwaShortcutManager {
    private static final String PREFS_NAME = "pwa_shortcuts_prefs";
    private static final String KEY_SHORTCUTS = "saved_shortcuts";

    public static void saveShortcut(Context context, ShortcutInfo shortcutInfo) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> saved = new HashSet<>(prefs.getStringSet(KEY_SHORTCUTS, new HashSet<>()));
        String entry = shortcutInfo.getPackage() + "|" + shortcutInfo.getId();
        saved.add(entry);
        prefs.edit().putStringSet(KEY_SHORTCUTS, saved).apply();
        Log.d("PwaShortcutManager", "Saved shortcut to app drawer: " + entry);
    }

    public static Set<String> getSavedShortcuts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_SHORTCUTS, new HashSet<>());
    }
}

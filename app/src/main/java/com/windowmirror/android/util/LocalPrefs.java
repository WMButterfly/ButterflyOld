package com.windowmirror.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.windowmirror.android.model.Entry;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for accessing local preferences.
 * @author alliecurry
 */
public final class LocalPrefs {
    private static final String PREFS_NAME = "wmprefs";
    private static final String KEY_ENTRIES = "wments";

    /** Local copy of History List as not to cause slow-down.
     * Should be saved when app is closed.
     * @see #storeEntries(Context) */
    private static List<Entry> HISTORY;

    private LocalPrefs() {
        throw new AssertionError();
    }

    private static SharedPreferences getPrefs(final Context context) {
        return context.getSharedPreferences(PREFS_NAME, 0);
    }

    public static List<Entry> getStoredEntries(final Context context) {
        if (HISTORY != null) {
            return HISTORY;
        }
        final String jsonString = getPrefs(context).getString(KEY_ENTRIES, "[]");
        final Type listType = new TypeToken<ArrayList<Entry>>(){}.getType();
        try {
            return HISTORY = getDefaultGson().fromJson(jsonString, listType);
        } catch (final JsonSyntaxException e) {
            Log.e("LocalPrefs", "Could not retrieve stored JSON: " + e.toString());
        }
        return HISTORY = new ArrayList<>();
    }

    public static void storeEntries(final Context context, final List<Entry> entries) {
        final String jsonString = getDefaultGson().toJson(entries);
        getPrefs(context).edit().putString(KEY_ENTRIES, jsonString).apply();
    }

    public static void storeEntries(final Context context) {
        if (HISTORY == null) {
            return; // Nothing to store...
        }
        storeEntries(context, HISTORY);
    }

    public static void addEntry(final Context context, final Entry entry) {
        if (HISTORY == null) {
            getStoredEntries(context);
        }
        HISTORY.add(0, entry);
    }

    private static Gson getDefaultGson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }
}

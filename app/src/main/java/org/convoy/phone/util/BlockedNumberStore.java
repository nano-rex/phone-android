package org.convoy.phone.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class BlockedNumberStore {
    private static final String PREFS = "blocked_numbers";
    private static final String KEY_NUMBERS = "numbers";

    private BlockedNumberStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String normalize(String number) {
        if (number == null) {
            return "";
        }
        return number.replaceAll("[^0-9+]", "");
    }

    public static boolean isBlocked(Context context, String number) {
        return prefs(context).getStringSet(KEY_NUMBERS, new HashSet<>()).contains(normalize(number));
    }

    public static void setBlocked(Context context, String number, boolean blocked) {
        Set<String> copy = new HashSet<>(prefs(context).getStringSet(KEY_NUMBERS, new HashSet<>()));
        String normalized = normalize(number);
        if (blocked) {
            copy.add(normalized);
        } else {
            copy.remove(normalized);
        }
        prefs(context).edit().putStringSet(KEY_NUMBERS, copy).apply();
    }
}

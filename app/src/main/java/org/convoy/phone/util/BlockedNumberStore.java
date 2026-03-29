package org.convoy.phone.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.BlockedNumberContract;
import android.provider.ContactsContract;

import java.util.HashSet;
import java.util.Set;

public final class BlockedNumberStore {
    private static final String PREFS = "blocked_numbers_fallback";
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

    public static boolean canUseSystemBlocklist(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }
        if (!DialerIntegration.isDefaultDialer(context)) {
            return false;
        }
        try {
            return BlockedNumberContract.canCurrentUserBlockNumbers(context);
        } catch (SecurityException e) {
            return false;
        }
    }

    public static boolean isHiddenNumber(String number) {
        String normalized = normalize(number);
        return normalized.isEmpty() || "unknown".equalsIgnoreCase(normalized) || "private".equalsIgnoreCase(normalized) || "restricted".equalsIgnoreCase(normalized);
    }

    public static boolean isInContacts(Context context, String number) {
        String normalized = normalize(number);
        if (normalized.isEmpty()) {
            return false;
        }
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalized));
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup._ID}, null, null, null)) {
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBlocked(Context context, String number) {
        String normalized = normalize(number);
        if (normalized.isEmpty()) {
            return false;
        }
        if (canUseSystemBlocklist(context)) {
            try {
                return BlockedNumberContract.isBlocked(context, normalized);
            } catch (SecurityException e) {
                return fallbackContains(context, normalized);
            }
        }
        return fallbackContains(context, normalized);
    }

    public static void setBlocked(Context context, String number, boolean blocked) {
        String normalized = normalize(number);
        if (normalized.isEmpty()) {
            return;
        }
        if (canUseSystemBlocklist(context)) {
            try {
                if (blocked) {
                    ContentValues values = new ContentValues();
                    values.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, normalized);
                    context.getContentResolver().insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values);
                } else {
                    BlockedNumberContract.unblock(context, normalized);
                }
                setFallbackBlocked(context, normalized, blocked);
                return;
            } catch (SecurityException ignored) {
            }
        }
        setFallbackBlocked(context, normalized, blocked);
    }

    private static boolean fallbackContains(Context context, String normalized) {
        return prefs(context).getStringSet(KEY_NUMBERS, new HashSet<>()).contains(normalized);
    }

    private static void setFallbackBlocked(Context context, String normalized, boolean blocked) {
        Set<String> copy = new HashSet<>(prefs(context).getStringSet(KEY_NUMBERS, new HashSet<>()));
        if (blocked) {
            copy.add(normalized);
        } else {
            copy.remove(normalized);
        }
        prefs(context).edit().putStringSet(KEY_NUMBERS, copy).apply();
    }
}

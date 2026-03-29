package org.convoy.phone.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.convoy.phone.model.RecentCallItem;

import java.util.ArrayList;
import java.util.List;

public final class ImportedCallHistoryStore {
    private static final String PREFS = "imported_call_history";
    private static final String KEY_DATA = "csv_data";

    private ImportedCallHistoryStore() {}

    public static void saveRaw(Context context, String raw) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DATA, raw == null ? "" : raw).commit();
    }

    public static String loadRaw(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DATA, "");
    }

    public static List<RecentCallItem> loadItems(Context context) {
        String raw = loadRaw(context);
        ArrayList<RecentCallItem> items = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return items;
        }
        String[] lines = raw.split("\\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",", 4);
            if (parts.length < 4) {
                continue;
            }
            long timestamp;
            try {
                timestamp = Long.parseLong(unescape(parts[0]));
            } catch (Exception e) {
                continue;
            }
            String name = unescape(parts[1]);
            String number = unescape(parts[2]);
            String details = unescape(parts[3]);
            items.add(new RecentCallItem(name == null || name.isEmpty() ? number : name, number, details, timestamp));
        }
        return items;
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace(",", "\\,").replace("\n", " ");
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder();
        boolean escape = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escape) {
                out.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}

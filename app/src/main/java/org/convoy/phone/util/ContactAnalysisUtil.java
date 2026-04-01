package org.convoy.phone.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;
import android.text.format.DateFormat;

import org.convoy.phone.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class ContactAnalysisUtil {
    private ContactAnalysisUtil() {
    }

    public static String buildAnalysis(Context context, String name, String number) {
        StringBuilder report = new StringBuilder();
        append(report, "Contact", safe(name));
        append(report, "Number", safe(number));
        append(report, "Blocked", BlockedNumberStore.isBlocked(context, number) ? "Yes" : "No");

        CallLogSummary summary = loadCallLogSummary(context, number);
        append(report, "Recent call count", String.valueOf(summary.count));
        append(report, "First seen", summary.firstSeen);
        append(report, "Last seen", summary.lastSeen);
        append(report, "Call pattern", summary.pattern);
        append(report, "Consistency note", buildConsistencyNote(summary, number));

        String telephonyInfo = TelephonyInfoCollector.collect(context);
        if (!telephonyInfo.isEmpty()) {
            report.append("\n\n")
                    .append(context.getString(R.string.current_network_info))
                    .append("\n")
                    .append(telephonyInfo);
        }

        report.append("\n\n")
                .append(context.getString(R.string.analysis_limit_note));
        return report.toString().trim();
    }

    private static void append(StringBuilder builder, String label, String value) {
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(label).append(": ").append(value == null || value.isEmpty() ? "Unavailable" : value);
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Unavailable" : value.trim();
    }

    private static CallLogSummary loadCallLogSummary(Context context, String number) {
        CallLogSummary summary = new CallLogSummary();
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            summary.pattern = "Grant call log permission for history analysis";
            return summary;
        }
        List<Integer> types = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE},
                null,
                null,
                CallLog.Calls.DATE + " DESC")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String loggedNumber = cursor.getString(0);
                    if (!sameNumber(loggedNumber, number)) {
                        continue;
                    }
                    long date = cursor.getLong(1);
                    int type = cursor.getInt(2);
                    types.add(type);
                    summary.count++;
                    if (summary.lastSeen == null) {
                        summary.lastSeen = formatDate(date);
                    }
                    summary.firstSeen = formatDate(date);
                }
            }
        } catch (Exception ignored) {
        }

        if (summary.count == 0) {
            summary.firstSeen = "No matching history";
            summary.lastSeen = "No matching history";
            summary.pattern = "No local call history for this number";
        } else {
            int incoming = 0;
            int outgoing = 0;
            int missed = 0;
            for (Integer type : types) {
                if (type == null) {
                    continue;
                }
                if (type == CallLog.Calls.INCOMING_TYPE) {
                    incoming++;
                } else if (type == CallLog.Calls.OUTGOING_TYPE) {
                    outgoing++;
                } else if (type == CallLog.Calls.MISSED_TYPE) {
                    missed++;
                }
            }
            summary.pattern = "Incoming " + incoming + ", outgoing " + outgoing + ", missed " + missed;
        }
        return summary;
    }

    private static boolean sameNumber(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return digitsOnly(a).equals(digitsOnly(b));
    }

    private static String digitsOnly(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isDigit(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static String formatDate(long millis) {
        return DateFormat.format("yyyy-MM-dd HH:mm", new Date(millis)).toString();
    }

    private static String buildConsistencyNote(CallLogSummary summary, String number) {
        if (number == null || number.trim().isEmpty()) {
            return "No contact number saved";
        }
        if (summary.count == 0) {
            return "No prior matching calls logged locally";
        }
        if (summary.count < 3) {
            return "Low history confidence: very few matching calls logged";
        }
        return "Number is locally consistent with repeated matching call history";
    }

    private static final class CallLogSummary {
        int count;
        String firstSeen = "Unavailable";
        String lastSeen = "Unavailable";
        String pattern = "Unavailable";
    }
}

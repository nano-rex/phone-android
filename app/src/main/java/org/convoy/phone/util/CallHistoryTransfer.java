package org.convoy.phone.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CallHistoryTransfer {
    private CallHistoryTransfer() {}

    public static boolean exportDeviceCallLog(Context context, Uri targetUri) {
        if (targetUri == null) {
            return false;
        }
        StringBuilder csv = new StringBuilder();
        csv.append("timestamp,name,number,details\n");
        try (Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.DURATION},
                null,
                null,
                CallLog.Calls.DATE + " DESC")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    long date = cursor.getLong(2);
                    int type = cursor.getInt(3);
                    long duration = cursor.getLong(4);
                    String detail = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(date)) + "  type=" + type + "  duration=" + duration + "s";
                    csv.append(date).append(',')
                            .append(ImportedCallHistoryStore.escape(name)).append(',')
                            .append(ImportedCallHistoryStore.escape(number)).append(',')
                            .append(ImportedCallHistoryStore.escape(detail)).append('\n');
                }
            }
            try (OutputStream out = context.getContentResolver().openOutputStream(targetUri, "w")) {
                if (out == null) {
                    return false;
                }
                out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
                out.flush();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean importCallLog(Context context, Uri sourceUri) {
        if (sourceUri == null) {
            return false;
        }
        try (InputStream in = context.getContentResolver().openInputStream(sourceUri)) {
            if (in == null) {
                return false;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            ImportedCallHistoryStore.saveRaw(context, buffer.toString(StandardCharsets.UTF_8.name()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

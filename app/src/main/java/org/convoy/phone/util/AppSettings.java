package org.convoy.phone.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.net.Uri;

public final class AppSettings {
    public static final String PREFS = "convoy_phone";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_RECORD_CALLS = "record_calls";
    private static final String KEY_RECORDING_SOURCE = "recording_source";
    private static final String KEY_RECORDINGS_TREE_URI = "recordings_tree_uri";

    public static final String SOURCE_ENVIRONMENT = "environment";
    public static final String SOURCE_DEVICE = "device";

    private AppSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isDarkMode(Context context) {
        return prefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkMode(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, value).apply();
    }

    public static boolean isRecordCallsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_RECORD_CALLS, false);
    }

    public static void setRecordCallsEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_RECORD_CALLS, value).apply();
    }

    public static String getRecordingSource(Context context) {
        return prefs(context).getString(KEY_RECORDING_SOURCE, SOURCE_ENVIRONMENT);
    }

    public static void setRecordingSource(Context context, String value) {
        prefs(context).edit().putString(KEY_RECORDING_SOURCE, value).apply();
    }

    public static Uri getRecordingsTreeUri(Context context) {
        String raw = prefs(context).getString(KEY_RECORDINGS_TREE_URI, null);
        return raw == null || raw.isEmpty() ? null : Uri.parse(raw);
    }

    public static void setRecordingsTreeUri(Context context, Uri uri) {
        prefs(context).edit().putString(KEY_RECORDINGS_TREE_URI, uri == null ? null : uri.toString()).apply();
    }

    public static int getMediaRecorderSource(Context context) {
        return SOURCE_DEVICE.equals(getRecordingSource(context))
                ? MediaRecorder.AudioSource.VOICE_RECOGNITION
                : MediaRecorder.AudioSource.MIC;
    }

    public static int[] getMediaRecorderSourceFallbacks(Context context) {
        if (SOURCE_DEVICE.equals(getRecordingSource(context))) {
            return new int[]{
                    MediaRecorder.AudioSource.VOICE_CALL,
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION
            };
        }
        return new int[]{
                MediaRecorder.AudioSource.MIC
        };
    }
}

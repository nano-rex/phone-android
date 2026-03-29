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
    private static final String KEY_COMPAT_TEST_PENDING = "compat_test_pending";
    private static final String KEY_BEST_RECORDER_SOURCE = "best_recorder_source";
    private static final String KEY_BLOCK_UNKNOWN_CALLERS = "block_unknown_callers";
    private static final String KEY_BLOCK_HIDDEN_CALLERS = "block_hidden_callers";

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
        prefs(context).edit().putBoolean(KEY_DARK_MODE, value).commit();
    }

    public static boolean isRecordCallsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_RECORD_CALLS, false);
    }

    public static void setRecordCallsEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_RECORD_CALLS, value).commit();
    }

    public static String getRecordingSource(Context context) {
        return prefs(context).getString(KEY_RECORDING_SOURCE, SOURCE_ENVIRONMENT);
    }

    public static void setRecordingSource(Context context, String value) {
        prefs(context).edit().putString(KEY_RECORDING_SOURCE, value).remove(KEY_BEST_RECORDER_SOURCE).commit();
    }

    public static Uri getRecordingsTreeUri(Context context) {
        String raw = prefs(context).getString(KEY_RECORDINGS_TREE_URI, null);
        return raw == null || raw.isEmpty() ? null : Uri.parse(raw);
    }

    public static void setRecordingsTreeUri(Context context, Uri uri) {
        prefs(context).edit().putString(KEY_RECORDINGS_TREE_URI, uri == null ? null : uri.toString()).commit();
    }

    public static int getMediaRecorderSource(Context context) {
        return SOURCE_DEVICE.equals(getRecordingSource(context))
                ? MediaRecorder.AudioSource.VOICE_RECOGNITION
                : MediaRecorder.AudioSource.MIC;
    }

    public static boolean isCompatibilityTestPending(Context context) {
        return prefs(context).getBoolean(KEY_COMPAT_TEST_PENDING, false);
    }

    public static void setCompatibilityTestPending(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_COMPAT_TEST_PENDING, value).commit();
    }

    public static Integer getBestRecorderSource(Context context) {
        if (!prefs(context).contains(KEY_BEST_RECORDER_SOURCE)) {
            return null;
        }
        return prefs(context).getInt(KEY_BEST_RECORDER_SOURCE, MediaRecorder.AudioSource.MIC);
    }

    public static void setBestRecorderSource(Context context, Integer value) {
        SharedPreferences.Editor editor = prefs(context).edit();
        if (value == null) {
            editor.remove(KEY_BEST_RECORDER_SOURCE);
        } else {
            editor.putInt(KEY_BEST_RECORDER_SOURCE, value);
        }
        editor.commit();
    }

    public static int[] getCompatibilityTestSources() {
        return new int[]{
                MediaRecorder.AudioSource.VOICE_CALL,
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.MIC
        };
    }

    public static int[] getMediaRecorderSourceFallbacks(Context context) {
        Integer best = getBestRecorderSource(context);
        if (best != null) {
            return new int[]{best};
        }
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

    public static boolean isBlockUnknownCallers(Context context) {
        return prefs(context).getBoolean(KEY_BLOCK_UNKNOWN_CALLERS, false);
    }

    public static void setBlockUnknownCallers(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_BLOCK_UNKNOWN_CALLERS, value).commit();
    }

    public static boolean isBlockHiddenCallers(Context context) {
        return prefs(context).getBoolean(KEY_BLOCK_HIDDEN_CALLERS, false);
    }

    public static void setBlockHiddenCallers(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_BLOCK_HIDDEN_CALLERS, value).commit();
    }

    public static String describeRecorderSource(int source) {
        switch (source) {
            case MediaRecorder.AudioSource.VOICE_CALL:
                return "Device voice path (VOICE_CALL)";
            case MediaRecorder.AudioSource.VOICE_COMMUNICATION:
                return "Voice communication";
            case MediaRecorder.AudioSource.VOICE_RECOGNITION:
                return "Voice recognition";
            case MediaRecorder.AudioSource.MIC:
                return "Environment mic";
            default:
                return "Unknown";
        }
    }
}

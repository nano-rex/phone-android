package org.convoy.phone.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import org.convoy.phone.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallRecordingService extends Service {
    public static final String ACTION_START = "org.convoy.phone.action.START_RECORDING";
    public static final String ACTION_STOP = "org.convoy.phone.action.STOP_RECORDING";
    public static final String EXTRA_FORCE_START = "force_start";
    private static final String CHANNEL_ID = "call_recording";
    private MediaRecorder recorder;
    private File tempOutputFile;
    private String outputDisplayName;
    private Integer activeSource;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            startForeground(1, buildNotification());
            startRecording(intent.getBooleanExtra(EXTRA_FORCE_START, false));
        } else if (ACTION_STOP.equals(action)) {
            stopRecording();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
        return START_STICKY;
    }

    private Notification buildNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.recording_service_channel), NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder.setContentTitle(getString(R.string.recording_service_channel))
                .setContentText(getString(R.string.recording_service_active))
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build();
    }

    private void startRecording(boolean forceStart) {
        boolean testPending = AppSettings.isCompatibilityTestPending(this);
        if ((!forceStart && !AppSettings.isRecordCallsEnabled(this)) || recorder != null) {
            return;
        }
        outputDisplayName = "call_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".m4a";
        tempOutputFile = new File(getCacheDir(), outputDisplayName);
        if (tempOutputFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tempOutputFile.delete();
        }
        boolean devicePathSelected = AppSettings.SOURCE_DEVICE.equals(AppSettings.getRecordingSource(this));
        int[] sources = testPending ? AppSettings.getCompatibilityTestSources() : AppSettings.getMediaRecorderSourceFallbacks(this);
        for (int source : sources) {
            try {
                recorder = new MediaRecorder();
                recorder.setAudioSource(source);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(44100);
                recorder.setAudioEncodingBitRate(128000);
                recorder.setOutputFile(tempOutputFile.getAbsolutePath());
                recorder.prepare();
                recorder.start();
                activeSource = source;
                if (testPending) {
                    AppSettings.setBestRecorderSource(this, source);
                    AppSettings.setCompatibilityTestPending(this, false);
                    Toast.makeText(this, getString(R.string.compatibility_test_result, AppSettings.describeRecorderSource(source)), Toast.LENGTH_LONG).show();
                }
                return;
            } catch (Exception ignored) {
                releaseRecorderOnly();
                if (tempOutputFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    tempOutputFile.delete();
                }
            }
        }
        if (testPending) {
            AppSettings.setCompatibilityTestPending(this, false);
        }
        Toast.makeText(this, devicePathSelected ? R.string.recording_device_path_unsupported : R.string.recording_start_failed, Toast.LENGTH_SHORT).show();
    }

    private void releaseRecorderOnly() {
        if (recorder != null) {
            try {
                recorder.release();
            } catch (Exception ignored) {
            }
            recorder = null;
        }
    }

    private void stopRecording() {
        boolean hadRecorder = recorder != null;
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (Exception ignored) {
            }
            releaseRecorderOnly();
        }
        if (hadRecorder && tempOutputFile != null && tempOutputFile.exists()) {
            boolean copied = StorageUtil.copyRecordingToFolder(this, tempOutputFile, outputDisplayName == null ? tempOutputFile.getName() : outputDisplayName);
            if (!copied) {
                Toast.makeText(this, R.string.recording_save_failed, Toast.LENGTH_SHORT).show();
            }
        }
        if (tempOutputFile != null && tempOutputFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tempOutputFile.delete();
        }
        tempOutputFile = null;
        outputDisplayName = null;
        activeSource = null;
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

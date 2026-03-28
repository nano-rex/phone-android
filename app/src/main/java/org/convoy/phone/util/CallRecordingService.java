package org.convoy.phone.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
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
            startRecording();
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

    private void startRecording() {
        if (!AppSettings.isRecordCallsEnabled(this) || recorder != null) {
            return;
        }
        outputDisplayName = "call_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".m4a";
        tempOutputFile = new File(getCacheDir(), outputDisplayName);
        if (tempOutputFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tempOutputFile.delete();
        }
        for (int source : AppSettings.getMediaRecorderSourceFallbacks(this)) {
            try {
                StorageUtil.writeTimestampedMarkerFile(this, "debug_record_attempt", "source=" + source + " file=" + outputDisplayName);
                recorder = new MediaRecorder();
                recorder.setAudioSource(source);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(44100);
                recorder.setAudioEncodingBitRate(128000);
                recorder.setOutputFile(tempOutputFile.getAbsolutePath());
                recorder.prepare();
                StorageUtil.writeTimestampedMarkerFile(this, "debug_record_prepare_ok", "source=" + source);
                recorder.start();
                activeSource = source;
                StorageUtil.writeTimestampedMarkerFile(this, "debug_record_start_ok", "source=" + source);
                return;
            } catch (Exception e) {
                StorageUtil.writeTimestampedMarkerFile(this, "debug_record_start_fail", "source=" + source + " error=" + String.valueOf(e));
                stopRecording();
            }
        }
        Toast.makeText(this, R.string.recording_start_failed, Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        boolean hadRecorder = recorder != null;
        if (recorder != null) {
            try {
                recorder.stop();
                StorageUtil.writeTimestampedMarkerFile(this, "debug_record_stop_ok", "source=" + String.valueOf(activeSource));
            } catch (Exception ignored) {
                StorageUtil.writeTimestampedMarkerFile(this, "debug_record_stop_fail", "source=" + String.valueOf(activeSource) + " error=" + String.valueOf(ignored));
            }
            try {
                recorder.release();
            } catch (Exception ignored) {
            }
            recorder = null;
        }
        if (hadRecorder && tempOutputFile != null && tempOutputFile.exists()) {
            boolean copied = StorageUtil.copyRecordingToFolder(this, tempOutputFile, outputDisplayName == null ? tempOutputFile.getName() : outputDisplayName);
            StorageUtil.writeTimestampedMarkerFile(this, copied ? "debug_record_copy_ok" : "debug_record_copy_fail",
                    "source=" + String.valueOf(activeSource) + " size=" + tempOutputFile.length());
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

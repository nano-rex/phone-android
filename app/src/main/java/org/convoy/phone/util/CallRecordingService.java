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
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import org.convoy.phone.R;

public class CallRecordingService extends Service {
    public static final String ACTION_START = "org.convoy.phone.action.START_RECORDING";
    public static final String ACTION_STOP = "org.convoy.phone.action.STOP_RECORDING";
    private static final String CHANNEL_ID = "call_recording";
    private MediaRecorder recorder;
    private ParcelFileDescriptor fileDescriptor;

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
        Uri uri = StorageUtil.createRecordingDocument(this);
        if (uri == null) {
            return;
        }
        for (int source : AppSettings.getMediaRecorderSourceFallbacks(this)) {
            try {
                fileDescriptor = getContentResolver().openFileDescriptor(uri, "w");
                if (fileDescriptor == null) {
                    return;
                }
                recorder = new MediaRecorder();
                recorder.setAudioSource(source);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(44100);
                recorder.setAudioEncodingBitRate(128000);
                recorder.setOutputFile(fileDescriptor.getFileDescriptor());
                recorder.prepare();
                recorder.start();
                return;
            } catch (Exception e) {
                stopRecording();
            }
        }
        Toast.makeText(this, R.string.recording_start_failed, Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (Exception ignored) {
            }
            try {
                recorder.release();
            } catch (Exception ignored) {
            }
            recorder = null;
        }
        if (fileDescriptor != null) {
            try {
                fileDescriptor.close();
            } catch (Exception ignored) {
            }
            fileDescriptor = null;
        }
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

package org.convoy.phone.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.convoy.phone.R;
import org.convoy.phone.util.AppSettings;
import org.convoy.phone.util.CallController;
import org.convoy.phone.util.BaseActivity;
import org.convoy.phone.util.CallRecordingService;
import org.convoy.phone.util.StorageUtil;

public class CallActivity extends BaseActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView nameView;
    private TextView stateView;
    private Button answerButton;
    private Button declineButton;
    private Button endButton;
    private Button muteButton;
    private Button speakerButton;

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshUi();
            if (CallController.getCurrentCall() != null) {
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        nameView = findViewById(R.id.call_name);
        stateView = findViewById(R.id.call_state);
        answerButton = findViewById(R.id.answer_button);
        declineButton = findViewById(R.id.decline_button);
        endButton = findViewById(R.id.end_button);
        muteButton = findViewById(R.id.mute_button);
        speakerButton = findViewById(R.id.speaker_button);

        answerButton.setOnClickListener(v -> {
            Call call = CallController.getCurrentCall();
            if (call != null) {
                maybeStartManualRecording();
                call.answer(0);
            }
        });
        declineButton.setOnClickListener(v -> {
            Call call = CallController.getCurrentCall();
            if (call != null) {
                call.reject(false, null);
            }
            stopManualRecordingAndClose();
        });
        endButton.setOnClickListener(v -> {
            Call call = CallController.getCurrentCall();
            if (call != null && call.getState() != Call.STATE_DISCONNECTED) {
                call.disconnect();
            }
            stopManualRecordingAndClose();
        });
        muteButton.setOnClickListener(v -> {
            if (!CallController.toggleMute()) {
                Toast.makeText(this, R.string.audio_route_unavailable, Toast.LENGTH_SHORT).show();
            }
            refreshUi();
        });
        speakerButton.setOnClickListener(v -> {
            if (!CallController.toggleSpeaker()) {
                Toast.makeText(this, R.string.audio_route_unavailable, Toast.LENGTH_SHORT).show();
            }
            refreshUi();
        });
    }

    private void maybeStartManualRecording() {
        if (!AppSettings.isRecordCallsEnabled(this)) {
            return;
        }
        StorageUtil.writeMarkerFile(this, "start.txt", "call started");
        Intent recordingIntent = new Intent(this, CallRecordingService.class);
        recordingIntent.setAction(CallRecordingService.ACTION_START);
        startForegroundService(recordingIntent);
    }

    private void stopManualRecordingAndClose() {
        StorageUtil.writeMarkerFile(this, "end.txt", "call ended");
        Intent recordingIntent = new Intent(this, CallRecordingService.class);
        recordingIntent.setAction(CallRecordingService.ACTION_STOP);
        startService(recordingIntent);
        CallController.closeEndedCall();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CallController.setActivityVisible(true);
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        CallController.setActivityVisible(false);
        handler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    private void refreshUi() {
        Call call = CallController.getCurrentCall();
        if (call == null && !CallController.isEndedAwaitingClose()) {
            finish();
            return;
        }
        nameView.setText(CallController.getDisplayHandle());
        int state = call == null ? CallController.getDisplayState() : call.getState();
        boolean ringing = state == Call.STATE_RINGING;
        stateView.setText(stateLabel(state));
        answerButton.setVisibility(ringing ? android.view.View.VISIBLE : android.view.View.GONE);
        declineButton.setVisibility(ringing ? android.view.View.VISIBLE : android.view.View.GONE);
        endButton.setVisibility(ringing ? android.view.View.GONE : android.view.View.VISIBLE);
        endButton.setText(state == Call.STATE_DISCONNECTED ? R.string.close : R.string.end_call);
        muteButton.setText(CallController.isMuted() ? R.string.unmute : R.string.mute);
        speakerButton.setText(CallController.isSpeakerOn() ? R.string.speaker_off : R.string.speaker);
        muteButton.setEnabled(state != Call.STATE_DISCONNECTED);
        speakerButton.setEnabled(state != Call.STATE_DISCONNECTED);
    }

    private String stateLabel(int state) {
        switch (state) {
            case Call.STATE_RINGING: return getString(R.string.call_state_incoming);
            case Call.STATE_ACTIVE: return getString(R.string.call_state_active);
            case Call.STATE_DIALING: return getString(R.string.call_state_dialing);
            case Call.STATE_CONNECTING: return getString(R.string.call_state_connecting);
            case Call.STATE_HOLDING: return getString(R.string.call_state_holding);
            case Call.STATE_DISCONNECTED: return getString(R.string.call_state_disconnected);
            default: return getString(R.string.call_state_unknown);
        }
    }
}

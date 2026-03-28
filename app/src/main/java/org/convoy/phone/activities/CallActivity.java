package org.convoy.phone.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.widget.Button;
import android.widget.TextView;

import org.convoy.phone.R;
import org.convoy.phone.util.CallController;

public class CallActivity extends Activity {
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
                call.answer(0);
            }
        });
        declineButton.setOnClickListener(v -> {
            Call call = CallController.getCurrentCall();
            if (call != null) {
                call.reject(false, null);
            }
            finish();
        });
        endButton.setOnClickListener(v -> {
            Call call = CallController.getCurrentCall();
            if (call != null) {
                call.disconnect();
            }
            finish();
        });
        muteButton.setOnClickListener(v -> {
            Call call = CallController.getCurrentCall();
            if (call != null && CallController.getCurrentAudioState() != null) {
                boolean next = !CallController.getCurrentAudioState().isMuted();
                if (call.getDetails() != null) {
                    // mute handled by InCallService via activity action bridge later; for now this is visual no-op if unsupported
                }
                stateView.setText(stateView.getText() + (next ? " / mute requested" : " / unmute requested"));
            }
        });
        speakerButton.setOnClickListener(v -> {
            stateView.setText(stateView.getText() + " / speaker requested");
        });
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
        if (call == null) {
            finish();
            return;
        }
        String handle = call.getDetails() != null && call.getDetails().getHandle() != null ? call.getDetails().getHandle().getSchemeSpecificPart() : "Unknown";
        nameView.setText(handle == null || handle.isEmpty() ? "Unknown" : handle);
        int state = call.getState();
        boolean ringing = state == Call.STATE_RINGING;
        nameView.setText(handle);
        stateView.setText(stateLabel(state));
        answerButton.setVisibility(ringing ? android.view.View.VISIBLE : android.view.View.GONE);
        declineButton.setVisibility(ringing ? android.view.View.VISIBLE : android.view.View.GONE);
        endButton.setVisibility(ringing ? android.view.View.GONE : android.view.View.VISIBLE);
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

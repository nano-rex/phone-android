package org.convoy.phone.util;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

public class ConvoyInCallService extends InCallService {
    private int trackedCalls;
    private boolean wroteStartMarker;

    @Override
    public void onCreate() {
        super.onCreate();
        CallController.setInCallService(this);
    }

    @Override
    public void onDestroy() {
        CallController.clearInCallService(this);
        super.onDestroy();
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        trackedCalls++;
        StorageUtil.writeTimestampedMarkerFile(this, "debug_call_added", "onCallAdded state=" + call.getState());
        String number = call.getDetails() != null && call.getDetails().getHandle() != null ? call.getDetails().getHandle().getSchemeSpecificPart() : "";
        if (call.getState() == Call.STATE_RINGING && BlockedNumberStore.isBlocked(this, number)) {
            try {
                call.reject(false, null);
            } catch (Exception ignored) {
            }
            return;
        }
        CallController.setCurrentCall(this, call);
        call.registerCallback(callCallback);
        maybeStartRecordingForState(call.getState());
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        StorageUtil.writeTimestampedMarkerFile(this, "debug_call_removed", "onCallRemoved state=" + call.getState());
        try {
            call.unregisterCallback(callCallback);
        } catch (Exception ignored) {
        }
        trackedCalls = Math.max(0, trackedCalls - 1);
        if (CallController.getCurrentCall() == call) {
            CallController.clearCall();
        }
        if (trackedCalls == 0) {
            stopRecordingService();
        }
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        super.onCallAudioStateChanged(audioState);
        CallController.setCurrentAudioState(audioState);
    }

    private final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            StorageUtil.writeTimestampedMarkerFile(
                    ConvoyInCallService.this,
                    "debug_call_state",
                    "state=" + state + " recordEnabled=" + AppSettings.isRecordCallsEnabled(ConvoyInCallService.this));
            maybeStartRecordingForState(state);
            if (state == Call.STATE_DISCONNECTED && trackedCalls <= 1) {
                stopRecordingService();
            }
        }
    };

    private void maybeStartRecordingForState(int state) {
        boolean recordEnabled = AppSettings.isRecordCallsEnabled(this);
        StorageUtil.writeTimestampedMarkerFile(this, "debug_record_enabled_read", "value=" + recordEnabled + " state=" + state);
        if (!recordEnabled) {
            StorageUtil.writeTimestampedMarkerFile(this, "debug_record_skip", "reason=disabled state=" + state);
            return;
        }
        if (state == Call.STATE_ACTIVE || state == Call.STATE_DIALING || state == Call.STATE_CONNECTING) {
            StorageUtil.writeTimestampedMarkerFile(this, "debug_record_trigger", "state=" + state);
            if (!wroteStartMarker) {
                StorageUtil.writeMarkerFile(this, "start.txt", "call started");
                wroteStartMarker = true;
            }
            Intent recordingIntent = new Intent(this, CallRecordingService.class);
            recordingIntent.setAction(CallRecordingService.ACTION_START);
            startForegroundService(recordingIntent);
        } else {
            StorageUtil.writeTimestampedMarkerFile(this, "debug_record_skip", "reason=state state=" + state);
        }
    }

    private void stopRecordingService() {
        if (wroteStartMarker) {
            StorageUtil.writeMarkerFile(this, "end.txt", "call ended");
            wroteStartMarker = false;
        }
        Intent recordingIntent = new Intent(this, CallRecordingService.class);
        recordingIntent.setAction(CallRecordingService.ACTION_STOP);
        startService(recordingIntent);
    }
}

package org.convoy.phone.util;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

public class ConvoyInCallService extends InCallService {
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
        String number = call.getDetails() != null && call.getDetails().getHandle() != null ? call.getDetails().getHandle().getSchemeSpecificPart() : "";
        if (call.getState() == Call.STATE_RINGING && BlockedNumberStore.isBlocked(this, number)) {
            try {
                call.reject(false, null);
            } catch (Exception ignored) {
            }
            return;
        }
        CallController.setCurrentCall(this, call);
        if (AppSettings.isRecordCallsEnabled(this)) {
            Intent recordingIntent = new Intent(this, CallRecordingService.class);
            recordingIntent.setAction(CallRecordingService.ACTION_START);
            startForegroundService(recordingIntent);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        if (CallController.getCurrentCall() == call) {
            CallController.clearCall();
        }
        Intent recordingIntent = new Intent(this, CallRecordingService.class);
        recordingIntent.setAction(CallRecordingService.ACTION_STOP);
        startService(recordingIntent);
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        super.onCallAudioStateChanged(audioState);
        CallController.setCurrentAudioState(audioState);
    }
}

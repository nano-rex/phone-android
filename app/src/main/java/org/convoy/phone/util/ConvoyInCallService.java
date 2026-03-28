package org.convoy.phone.util;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

public class ConvoyInCallService extends InCallService {
    private int trackedCalls;

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
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        try {
            call.unregisterCallback(callCallback);
        } catch (Exception ignored) {
        }
        trackedCalls = Math.max(0, trackedCalls - 1);
        if (CallController.getCurrentCall() == call) {
            CallController.markCallRemoved();
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
            if (state == Call.STATE_DISCONNECTED) {
                CallController.markEndedAwaitingClose();
            }
        }
    };
}

package org.convoy.phone.util;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

public class ConvoyInCallService extends InCallService {
    private int trackedCalls;
    private Ringtone ringtone;

    @Override
    public void onCreate() {
        super.onCreate();
        CallController.setInCallService(this);
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
        updateRingtoneForState(call.getState());
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
        updateRingtoneForState(Call.STATE_DISCONNECTED);
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
            updateRingtoneForState(state);
            if (state == Call.STATE_DISCONNECTED) {
                CallController.markEndedAwaitingClose();
            }
        }
    };

    private void updateRingtoneForState(int state) {
        if (state == Call.STATE_RINGING) {
            startRingtone();
        } else {
            stopRingtone();
        }
    }

    private void startRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            return;
        }
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        if (uri == null) {
            return;
        }
        ringtone = RingtoneManager.getRingtone(this, uri);
        if (ringtone != null) {
            ringtone.play();
        }
    }

    private void stopRingtone() {
        if (ringtone != null) {
            try {
                ringtone.stop();
            } catch (Exception ignored) {
            }
            ringtone = null;
        }
    }

    @Override
    public void onDestroy() {
        stopRingtone();
        CallController.clearInCallService(this);
        super.onDestroy();
    }
}

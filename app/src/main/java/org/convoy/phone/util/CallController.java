package org.convoy.phone.util;

import android.content.Context;
import android.content.Intent;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

import org.convoy.phone.activities.CallActivity;

public final class CallController {
    private static Call currentCall;
    private static CallAudioState currentAudioState;
    private static ConvoyInCallService inCallService;
    private static boolean activityVisible;

    private CallController() {}

    public static void setInCallService(ConvoyInCallService service) {
        inCallService = service;
    }

    public static void clearInCallService(ConvoyInCallService service) {
        if (inCallService == service) {
            inCallService = null;
        }
    }

    public static void setCurrentCall(Context context, Call call) {
        if (currentCall != null && currentCall != call) {
            try {
                currentCall.unregisterCallback(CALLBACK);
            } catch (Exception ignored) {
            }
        }
        currentCall = call;
        if (currentCall != null) {
            currentCall.registerCallback(CALLBACK);
            launchCallUi(context);
        }
    }

    public static Call getCurrentCall() {
        return currentCall;
    }

    public static void clearCall() {
        if (currentCall != null) {
            try {
                currentCall.unregisterCallback(CALLBACK);
            } catch (Exception ignored) {
            }
        }
        currentCall = null;
        currentAudioState = null;
    }

    public static void setCurrentAudioState(CallAudioState audioState) {
        currentAudioState = audioState;
    }

    public static CallAudioState getCurrentAudioState() {
        return currentAudioState;
    }

    public static void setActivityVisible(boolean visible) {
        activityVisible = visible;
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void launchCallUi(Context context) {
        Intent intent = new Intent(context, CallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static boolean toggleMute() {
        if (inCallService == null || currentAudioState == null) {
            return false;
        }
        boolean target = !currentAudioState.isMuted();
        inCallService.setMuted(target);
        return true;
    }

    public static boolean toggleSpeaker() {
        if (inCallService == null || currentAudioState == null) {
            return false;
        }
        int route = currentAudioState.getRoute() == CallAudioState.ROUTE_SPEAKER
                ? CallAudioState.ROUTE_EARPIECE
                : CallAudioState.ROUTE_SPEAKER;
        inCallService.setAudioRoute(route);
        return true;
    }

    public static boolean isMuted() {
        return currentAudioState != null && currentAudioState.isMuted();
    }

    public static boolean isSpeakerOn() {
        return currentAudioState != null && currentAudioState.getRoute() == CallAudioState.ROUTE_SPEAKER;
    }

    public static final Call.Callback CALLBACK = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            if (state == Call.STATE_DISCONNECTED) {
                clearCall();
            }
        }
    };
}

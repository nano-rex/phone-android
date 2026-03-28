package org.convoy.phone.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PhoneStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        Intent serviceIntent = new Intent(context, CallRecordingService.class);
        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state) && AppSettings.isRecordCallsEnabled(context)) {
            serviceIntent.setAction(CallRecordingService.ACTION_START);
            context.startForegroundService(serviceIntent);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            serviceIntent.setAction(CallRecordingService.ACTION_STOP);
            context.startService(serviceIntent);
        }
    }
}

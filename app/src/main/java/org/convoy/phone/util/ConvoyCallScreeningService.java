package org.convoy.phone.util;

import android.telecom.Call;
import android.telecom.CallScreeningService;

public class ConvoyCallScreeningService extends CallScreeningService {
    @Override
    public void onScreenCall(Call.Details callDetails) {
        String number = callDetails.getHandle() != null ? callDetails.getHandle().getSchemeSpecificPart() : "";
        CallResponse.Builder builder = new CallResponse.Builder();
        if (BlockedNumberStore.isBlocked(this, number)) {
            respondToCall(callDetails, builder.setDisallowCall(true).setRejectCall(true).setSkipCallLog(false).setSkipNotification(true).build());
        } else {
            respondToCall(callDetails, builder.setDisallowCall(false).setRejectCall(false).build());
        }
    }
}

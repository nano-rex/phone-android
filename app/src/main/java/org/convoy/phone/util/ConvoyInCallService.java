package org.convoy.phone.util;

import android.telecom.Call;
import android.telecom.InCallService;

public class ConvoyInCallService extends InCallService {
    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
    }
}

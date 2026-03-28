package org.convoy.phone.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.widget.Toast;

import org.convoy.phone.R;
import org.convoy.phone.util.BaseActivity;
import org.convoy.phone.util.DialerIntegration;

public class DialerActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent source) {
        Uri data = source == null ? null : source.getData();
        if (data == null) {
            finish();
            return;
        }

        if (DialerIntegration.isDefaultDialer(this)) {
            TelecomManager telecomManager = getSystemService(TelecomManager.class);
            if (telecomManager != null) {
                telecomManager.placeCall(data, null);
                finish();
                return;
            }
        }

        Toast.makeText(this, R.string.set_default_dialer_to_call, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setData(data);
        startActivity(intent);
        finish();
    }
}

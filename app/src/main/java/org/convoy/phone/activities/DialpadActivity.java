package org.convoy.phone.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.convoy.phone.util.BaseActivity;

public class DialpadActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forwardToMain(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        forwardToMain(intent);
    }

    private void forwardToMain(Intent source) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Uri data = source == null ? null : source.getData();
        if (data != null) {
            intent.setData(data);
        }
        startActivity(intent);
        finish();
    }
}

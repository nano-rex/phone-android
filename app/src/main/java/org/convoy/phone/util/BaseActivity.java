package org.convoy.phone.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.widget.Button;
import android.util.TypedValue;
import android.widget.Toast;

import org.convoy.phone.R;
import org.convoy.phone.activities.ContactsActivity;
import org.convoy.phone.activities.MainActivity;
import org.convoy.phone.activities.RecordingsActivity;
import org.convoy.phone.activities.RecentsActivity;
public abstract class BaseActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(AppSettings.isDarkMode(this) ? R.style.Theme_ConvoyPhone_Dark : R.style.Theme_ConvoyPhone_Light);
        super.onCreate(savedInstanceState);
    }

    protected void bindBottomNav(int selectedButtonId) {
        int[] ids = new int[]{R.id.tab_home, R.id.tab_contacts, R.id.tab_recents, R.id.tab_recordings};
        Class<?>[] targets = new Class[]{MainActivity.class, ContactsActivity.class, RecentsActivity.class, RecordingsActivity.class};
        int widthDp = Math.round(getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
        int minHeightDp = widthDp < 360 ? 60 : 56;
        int maxTextSp = widthDp < 360 ? 12 : 13;
        int minTextSp = widthDp < 360 ? 9 : 10;
        for (int i = 0; i < ids.length; i++) {
            Button button = findViewById(ids[i]);
            if (button == null) {
                continue;
            }
            button.setAllCaps(false);
            button.setSingleLine(false);
            button.setMaxLines(2);
            button.setMinLines(2);
            button.setGravity(android.view.Gravity.CENTER);
            button.setMinHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minHeightDp, getResources().getDisplayMetrics()));
            button.setAutoSizeTextTypeUniformWithConfiguration(minTextSp, maxTextSp, 1, TypedValue.COMPLEX_UNIT_SP);
            button.setEnabled(ids[i] != selectedButtonId);
            final Class<?> target = targets[i];
            button.setOnClickListener(v -> {
                if (!target.equals(getClass())) {
                    startActivity(new Intent(this, target));
                    finish();
                }
            });
        }
    }

    protected void shareToClipboard(String label, String value) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, value));
    }

    protected void dialNumber(String number) {
        if (BlockedNumberStore.isBlocked(this, number)) {
            Toast.makeText(this, R.string.number_is_blocked, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.parse("tel:" + Uri.encode(number));
        if (DialerIntegration.isDefaultDialer(this)) {
            TelecomManager telecomManager = getSystemService(TelecomManager.class);
            if (telecomManager != null) {
                startManualRecording("dial");
                telecomManager.placeCall(uri, null);
                return;
            }
        }
        Toast.makeText(this, R.string.set_default_dialer_to_call, Toast.LENGTH_SHORT).show();
        startActivity(DialerIntegration.createRoleRequestIntent(this));
    }

    protected void startManualRecording(String source) {
        if (!AppSettings.isRecordCallsEnabled(this)) {
            StorageUtil.writeTimestampedMarkerFile(this, "debug_manual_record_skip", "source=" + source + " reason=disabled");
            return;
        }
        boolean wrote = StorageUtil.writeMarkerFile(this, "start.txt", "call started");
        StorageUtil.writeTimestampedMarkerFile(this, "debug_manual_record_start", "source=" + source + " wroteStart=" + wrote);
        try {
            Intent recordingIntent = new Intent(this, CallRecordingService.class);
            recordingIntent.setAction(CallRecordingService.ACTION_START);
            recordingIntent.putExtra(CallRecordingService.EXTRA_FORCE_START, true);
            startForegroundService(recordingIntent);
            StorageUtil.writeTimestampedMarkerFile(this, "debug_manual_record_service", "source=" + source + " started=true");
        } catch (Exception e) {
            StorageUtil.writeTimestampedMarkerFile(this, "debug_manual_record_service", "source=" + source + " started=false error=" + String.valueOf(e));
        }
    }

    protected void stopManualRecording(String source) {
        boolean wrote = StorageUtil.writeMarkerFile(this, "end.txt", "call ended");
        StorageUtil.writeTimestampedMarkerFile(this, "debug_manual_record_stop", "source=" + source + " wroteEnd=" + wrote);
        try {
            Intent recordingIntent = new Intent(this, CallRecordingService.class);
            recordingIntent.setAction(CallRecordingService.ACTION_STOP);
            startService(recordingIntent);
            StorageUtil.writeTimestampedMarkerFile(this, "debug_manual_record_service", "source=" + source + " started=true");
        } catch (Exception e) {
            StorageUtil.writeTimestampedMarkerFile(this, "debug_manual_record_service", "source=" + source + " started=false error=" + String.valueOf(e));
        }
    }
}

package org.convoy.phone.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import org.convoy.phone.R;
import org.convoy.phone.activities.ContactsActivity;
import org.convoy.phone.activities.MainActivity;
import org.convoy.phone.activities.RecordingsActivity;
import org.convoy.phone.activities.RecentsActivity;
import org.convoy.phone.activities.SettingsActivity;

public abstract class BaseActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(AppSettings.isDarkMode(this) ? R.style.Theme_ConvoyPhone_Dark : R.style.Theme_ConvoyPhone_Light);
        super.onCreate(savedInstanceState);
    }

    protected void bindBottomNav(int selectedButtonId) {
        int[] ids = new int[]{R.id.tab_home, R.id.tab_contacts, R.id.tab_recents, R.id.tab_recordings, R.id.tab_settings};
        Class<?>[] targets = new Class[]{MainActivity.class, ContactsActivity.class, RecentsActivity.class, RecordingsActivity.class, SettingsActivity.class};
        for (int i = 0; i < ids.length; i++) {
            Button button = findViewById(ids[i]);
            if (button == null) {
                continue;
            }
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
        Intent intent = new Intent(Intent.ACTION_CALL, android.net.Uri.parse("tel:" + android.net.Uri.encode(number)));
        startActivity(intent);
    }
}

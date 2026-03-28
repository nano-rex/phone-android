package org.convoy.phone.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.convoy.phone.R;
import org.convoy.phone.util.AppSettings;
import org.convoy.phone.util.BaseActivity;
import org.convoy.phone.util.DialerIntegration;
import org.convoy.phone.util.BlockedNumberStore;
import org.convoy.phone.util.StorageUtil;

public class SettingsActivity extends BaseActivity {
    private static final int REQ_FOLDER = 4;
    private static final int REQ_AUDIO = 5;
    private TextView folderStatus;
    private TextView defaultDialerStatus;
    private TextView blocklistStatus;
    private Switch recordCallsSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        bindBottomNav(0);
        folderStatus = findViewById(R.id.folder_status);
        defaultDialerStatus = findViewById(R.id.default_dialer_status);
        blocklistStatus = findViewById(R.id.blocklist_status);

        Switch darkMode = findViewById(R.id.dark_mode_switch);
        darkMode.setChecked(AppSettings.isDarkMode(this));
        darkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSettings.setDarkMode(this, isChecked);
            recreate();
        });

        recordCallsSwitch = findViewById(R.id.record_calls_switch);
        recordCallsSwitch.setChecked(AppSettings.isRecordCallsEnabled(this));
        StorageUtil.writeTimestampedMarkerFile(this, "debug_setting_record_calls", "onCreate value=" + AppSettings.isRecordCallsEnabled(this));
        recordCallsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE}, REQ_AUDIO);
            }
            AppSettings.setRecordCallsEnabled(this, isChecked);
            StorageUtil.writeTimestampedMarkerFile(this, "debug_setting_record_calls", "toggle value=" + isChecked + " stored=" + AppSettings.isRecordCallsEnabled(this));
        });

        RadioGroup sourceGroup = findViewById(R.id.recording_source_group);
        sourceGroup.check(AppSettings.SOURCE_DEVICE.equals(AppSettings.getRecordingSource(this)) ? R.id.source_device : R.id.source_environment);
        sourceGroup.setOnCheckedChangeListener((group, checkedId) -> AppSettings.setRecordingSource(this,
                checkedId == R.id.source_device ? AppSettings.SOURCE_DEVICE : AppSettings.SOURCE_ENVIRONMENT));

        findViewById(R.id.choose_folder_button).setOnClickListener(v -> openFolderPicker());
        findViewById(R.id.write_test_file_button).setOnClickListener(v -> {
            boolean ok = StorageUtil.writeMarkerFile(this, "probe.txt", "folder write test");
            Toast.makeText(this, ok ? R.string.test_file_written : R.string.test_file_failed, Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.request_default_dialer_button).setOnClickListener(v -> {
            boolean launched = DialerIntegration.requestDefaultDialer(this);
            if (!launched) {
                Toast.makeText(this, R.string.default_dialer_request_unavailable, Toast.LENGTH_SHORT).show();
            }
        });
        updateFolderStatus();
        updateDialerStatus();
        updateBlocklistStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDialerStatus();
        updateBlocklistStatus();
        StorageUtil.writeTimestampedMarkerFile(this, "debug_setting_record_calls", "onResume value=" + AppSettings.isRecordCallsEnabled(this));
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FOLDER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            AppSettings.setRecordingsTreeUri(this, uri);
            updateFolderStatus();
        }
        if (requestCode == DialerIntegration.REQ_DEFAULT_DIALER) {
            updateDialerStatus();
        updateBlocklistStatus();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (!granted) {
                AppSettings.setRecordCallsEnabled(this, false);
                if (recordCallsSwitch != null) {
                    recordCallsSwitch.setChecked(false);
                }
            }
            StorageUtil.writeTimestampedMarkerFile(this, "debug_setting_record_calls", "permission_result granted=" + granted + " stored=" + AppSettings.isRecordCallsEnabled(this));
        }
    }

    private void updateFolderStatus() {
        folderStatus.setText(AppSettings.getRecordingsTreeUri(this) == null ? R.string.folder_not_set : R.string.folder_set);
    }

    private void updateDialerStatus() {
        defaultDialerStatus.setText(DialerIntegration.isDefaultDialer(this)
                ? R.string.default_dialer_enabled
                : R.string.default_dialer_disabled);
    }

    private void updateBlocklistStatus() {
        blocklistStatus.setText(BlockedNumberStore.canUseSystemBlocklist(this)
                ? R.string.system_blocklist_enabled
                : R.string.system_blocklist_fallback);
    }
}

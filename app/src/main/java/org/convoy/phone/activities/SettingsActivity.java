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

public class SettingsActivity extends BaseActivity {
    private static final int REQ_FOLDER = 4;
    private static final int REQ_AUDIO = 5;
    private TextView folderStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        bindBottomNav(R.id.tab_settings);
        folderStatus = findViewById(R.id.folder_status);

        Switch darkMode = findViewById(R.id.dark_mode_switch);
        darkMode.setChecked(AppSettings.isDarkMode(this));
        darkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSettings.setDarkMode(this, isChecked);
            recreate();
        });

        Switch recordCalls = findViewById(R.id.record_calls_switch);
        recordCalls.setChecked(AppSettings.isRecordCallsEnabled(this));
        recordCalls.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE}, REQ_AUDIO);
            }
            AppSettings.setRecordCallsEnabled(this, isChecked);
        });

        RadioGroup sourceGroup = findViewById(R.id.recording_source_group);
        sourceGroup.check(AppSettings.SOURCE_DEVICE.equals(AppSettings.getRecordingSource(this)) ? R.id.source_device : R.id.source_environment);
        sourceGroup.setOnCheckedChangeListener((group, checkedId) -> AppSettings.setRecordingSource(this,
                checkedId == R.id.source_device ? AppSettings.SOURCE_DEVICE : AppSettings.SOURCE_ENVIRONMENT));

        findViewById(R.id.choose_folder_button).setOnClickListener(v -> openFolderPicker());
        updateFolderStatus();
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
    }

    private void updateFolderStatus() {
        folderStatus.setText(AppSettings.getRecordingsTreeUri(this) == null ? R.string.folder_not_set : R.string.folder_set);
    }
}

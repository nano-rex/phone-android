package org.convoy.phone.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.convoy.phone.R;
import org.convoy.phone.util.BaseActivity;

public class MainActivity extends BaseActivity {
    private static final int REQ_CALL = 1;
    private EditText numberInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        numberInput = findViewById(R.id.number_input);
        bindBottomNav(R.id.tab_home);
        setupDigits();
        findViewById(R.id.call_button).setOnClickListener(v -> placeCall());
        findViewById(R.id.clear_button).setOnClickListener(v -> numberInput.setText(""));
    }

    private void setupDigits() {
        int[] ids = new int[]{R.id.digit_0, R.id.digit_1, R.id.digit_2, R.id.digit_3, R.id.digit_4, R.id.digit_5, R.id.digit_6, R.id.digit_7, R.id.digit_8, R.id.digit_9, R.id.digit_star, R.id.digit_hash};
        for (int id : ids) {
            Button button = findViewById(id);
            button.setOnClickListener(v -> numberInput.append(button.getText()));
        }
    }

    private void placeCall() {
        String number = numberInput.getText().toString().trim();
        if (TextUtils.isEmpty(number)) {
            Toast.makeText(this, R.string.empty_number, Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQ_CALL);
            return;
        }
        dialNumber(number);
    }
}

package org.convoy.phone.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.convoy.phone.R;
import org.convoy.phone.util.BaseActivity;
import org.convoy.phone.util.BlockedNumberStore;

public class ContactDetailActivity extends BaseActivity {
    public static final String EXTRA_CONTACT_ID = "contact_id";
    public static final String EXTRA_CONTACT_URI = "contact_uri";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_NUMBER = "number";
    public static final String EXTRA_FAVORITE = "favorite";

    private long contactId;
    private Uri contactUri;
    private String name;
    private String number;
    private boolean favorite;
    private TextView nameView;
    private TextView numberView;
    private Button favoriteButton;
    private Button blockButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_detail);

        contactId = getIntent().getLongExtra(EXTRA_CONTACT_ID, -1L);
        String rawUri = getIntent().getStringExtra(EXTRA_CONTACT_URI);
        contactUri = rawUri == null ? null : Uri.parse(rawUri);
        name = getIntent().getStringExtra(EXTRA_NAME);
        number = getIntent().getStringExtra(EXTRA_NUMBER);
        favorite = getIntent().getBooleanExtra(EXTRA_FAVORITE, false);

        nameView = findViewById(R.id.contact_detail_name);
        numberView = findViewById(R.id.contact_detail_number);
        favoriteButton = findViewById(R.id.contact_detail_favorite);
        blockButton = findViewById(R.id.contact_detail_block);

        findViewById(R.id.contact_detail_call).setOnClickListener(v -> dialNumber(number));
        findViewById(R.id.contact_detail_copy).setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText(getString(R.string.copy_number), number));
            Toast.makeText(this, R.string.copy_number, Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.contact_detail_edit).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setDataAndType(contactUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE);
            intent.putExtra("finishActivityOnSaveCompleted", true);
            startActivity(intent);
        });
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        blockButton.setOnClickListener(v -> toggleBlocked());

        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void toggleFavorite() {
        if (contactId <= 0) {
            return;
        }
        favorite = !favorite;
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(ContactsContract.Contacts.STARRED, favorite ? 1 : 0);
        getContentResolver().update(ContactsContract.Contacts.CONTENT_URI, values, ContactsContract.Contacts._ID + "=?", new String[]{String.valueOf(contactId)});
        render();
    }

    private void toggleBlocked() {
        boolean blocked = BlockedNumberStore.isBlocked(this, number);
        BlockedNumberStore.setBlocked(this, number, !blocked);
        render();
    }

    private void render() {
        nameView.setText(name == null || name.isEmpty() ? getString(R.string.unknown_contact) : name);
        numberView.setText(number == null ? "" : number);
        favoriteButton.setText(favorite ? R.string.remove_favorite : R.string.add_favorite);
        blockButton.setText(BlockedNumberStore.isBlocked(this, number) ? R.string.unblock_number : R.string.block_number);
    }
}

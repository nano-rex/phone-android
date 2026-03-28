package org.convoy.phone.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.convoy.phone.R;
import org.convoy.phone.model.ContactItem;
import org.convoy.phone.util.BaseActivity;
import org.convoy.phone.util.BlockedNumberStore;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends BaseActivity {
    private static final int REQ_CONTACTS = 2;
    private final List<ContactItem> allContacts = new ArrayList<>();
    private final List<ContactItem> filteredContacts = new ArrayList<>();
    private ArrayAdapter<ContactItem> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        bindBottomNav(R.id.tab_contacts);

        ListView listView = findViewById(R.id.contacts_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredContacts);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> dialNumber(filteredContacts.get(position).number));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showContactActions(filteredContacts.get(position));
            return true;
        });

        EditText search = findViewById(R.id.search_contacts);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.new_contact_button).setOnClickListener(v -> {
            Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
            startActivity(intent);
        });

        loadContacts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContacts();
    }

    private void showContactActions(ContactItem item) {
        boolean blocked = BlockedNumberStore.isBlocked(this, item.number);
        String[] actions = new String[]{getString(R.string.call), getString(R.string.edit_contact), getString(R.string.copy_number), blocked ? getString(R.string.unblock_number) : getString(R.string.block_number)};
        new AlertDialog.Builder(this)
                .setTitle(item.name)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        dialNumber(item.number);
                    } else if (which == 1) {
                        Intent intent = new Intent(Intent.ACTION_EDIT);
                        intent.setDataAndType(item.contactUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                        intent.putExtra("finishActivityOnSaveCompleted", true);
                        startActivity(intent);
                    } else if (which == 2) {
                        shareToClipboard(getString(R.string.copy_number), item.number);
                        Toast.makeText(this, R.string.copy_number, Toast.LENGTH_SHORT).show();
                    } else {
                        BlockedNumberStore.setBlocked(this, item.number, !blocked);
                        Toast.makeText(this, blocked ? R.string.unblock_number : R.string.block_number, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void loadContacts() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQ_CONTACTS);
            return;
        }
        allContacts.clear();
        try (Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.CONTACT_ID},
                null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long contactId = cursor.getLong(2);
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contactId));
                    allContacts.add(new ContactItem(cursor.getString(0), cursor.getString(1), uri));
                }
            }
        }
        filter("");
    }

    private void filter(String query) {
        filteredContacts.clear();
        String q = query == null ? "" : query.toLowerCase();
        for (ContactItem item : allContacts) {
            if (q.isEmpty() || item.name.toLowerCase().contains(q) || item.number.contains(q)) {
                filteredContacts.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        findViewById(R.id.empty_contacts).setVisibility(filteredContacts.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}

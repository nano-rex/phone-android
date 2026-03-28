package org.convoy.phone.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
            shareToClipboard(getString(R.string.copy_number), filteredContacts.get(position).number);
            Toast.makeText(this, R.string.copy_number, Toast.LENGTH_SHORT).show();
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

    private void loadContacts() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQ_CONTACTS);
            return;
        }
        allContacts.clear();
        try (Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    allContacts.add(new ContactItem(cursor.getString(0), cursor.getString(1)));
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

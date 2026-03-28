package org.convoy.phone.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.convoy.phone.R;
import org.convoy.phone.model.ContactItem;
import org.convoy.phone.util.BaseActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ContactsActivity extends BaseActivity {
    private static final int REQ_CONTACTS = 2;
    private final List<ContactItem> allContacts = new ArrayList<>();
    private final List<ContactItem> filteredContacts = new ArrayList<>();
    private ArrayAdapter<ContactItem> adapter;
    private EditText searchView;
    private boolean favoritesOnly;
    private boolean favoritesFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        bindBottomNav(R.id.tab_contacts);

        searchView = findViewById(R.id.search_contacts);
        ListView listView = findViewById(R.id.contacts_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredContacts);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> openDetails(filteredContacts.get(position)));

        searchView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.new_contact_button).setOnClickListener(v -> {
            Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
            startActivity(intent);
        });

        Button favoritesButton = findViewById(R.id.favorites_filter_button);
        favoritesButton.setOnClickListener(v -> {
            favoritesOnly = !favoritesOnly;
            favoritesButton.setText(favoritesOnly ? R.string.show_all_contacts : R.string.show_favorites_only);
            applyFilters();
        });

        Button sortButton = findViewById(R.id.sort_contacts_button);
        sortButton.setOnClickListener(v -> {
            favoritesFirst = !favoritesFirst;
            sortButton.setText(favoritesFirst ? R.string.sort_favorites_first : R.string.sort_name_only);
            applyFilters();
        });

        loadContacts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContacts();
    }

    private void openDetails(ContactItem item) {
        Intent intent = new Intent(this, ContactDetailActivity.class);
        intent.putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, item.contactId);
        intent.putExtra(ContactDetailActivity.EXTRA_CONTACT_URI, item.contactUri == null ? null : item.contactUri.toString());
        intent.putExtra(ContactDetailActivity.EXTRA_NAME, item.name);
        intent.putExtra(ContactDetailActivity.EXTRA_NUMBER, item.number);
        intent.putExtra(ContactDetailActivity.EXTRA_FAVORITE, item.favorite);
        startActivity(intent);
    }

    private void loadContacts() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, REQ_CONTACTS);
            return;
        }
        allContacts.clear();
        try (Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                        ContactsContract.CommonDataKinds.Phone.STARRED
                },
                null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String displayName = cursor.getString(0);
                    String number = cursor.getString(1);
                    long contactId = cursor.getLong(2);
                    String lookupKey = cursor.getString(3);
                    boolean favorite = cursor.getInt(4) == 1;
                    Uri uri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
                    allContacts.add(new ContactItem(contactId, displayName, number, uri, favorite));
                }
            }
        }
        applyFilters();
    }

    private void applyFilters() {
        filteredContacts.clear();
        String query = searchView.getText() == null ? "" : searchView.getText().toString().trim().toLowerCase(Locale.US);
        for (ContactItem item : allContacts) {
            if (favoritesOnly && !item.favorite) {
                continue;
            }
            if (!query.isEmpty()) {
                String name = item.name == null ? "" : item.name.toLowerCase(Locale.US);
                String number = item.number == null ? "" : item.number;
                if (!name.contains(query) && !number.contains(query)) {
                    continue;
                }
            }
            filteredContacts.add(item);
        }
        Collections.sort(filteredContacts, new Comparator<ContactItem>() {
            @Override
            public int compare(ContactItem a, ContactItem b) {
                if (favoritesFirst && a.favorite != b.favorite) {
                    return a.favorite ? -1 : 1;
                }
                String an = a.name == null ? "" : a.name.toLowerCase(Locale.US);
                String bn = b.name == null ? "" : b.name.toLowerCase(Locale.US);
                return an.compareTo(bn);
            }
        });
        adapter.notifyDataSetChanged();
        findViewById(R.id.empty_contacts).setVisibility(filteredContacts.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}

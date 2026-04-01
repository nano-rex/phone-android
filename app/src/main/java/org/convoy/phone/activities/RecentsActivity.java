package org.convoy.phone.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.provider.CallLog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.convoy.phone.R;
import org.convoy.phone.adapters.RecentCallAdapter;
import org.convoy.phone.model.RecentCallItem;
import org.convoy.phone.util.BaseActivity;
import org.convoy.phone.util.BlockedNumberStore;
import org.convoy.phone.util.ContactAnalysisUtil;
import org.convoy.phone.util.ImportedCallHistoryStore;
import org.convoy.phone.util.TelephonyInfoCollector;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentsActivity extends BaseActivity {
    private static final int REQ_CALL_LOG = 3;
    private final List<RecentCallItem> items = new ArrayList<>();
    private final List<RecentCallItem> allItems = new ArrayList<>();
    private ArrayAdapter<RecentCallItem> adapter;
    private EditText searchView;
    private Button fromDateButton;
    private Button toDateButton;
    private Long fromDateMillis;
    private Long toDateMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recents);
        bindBottomNav(R.id.tab_recents);
        ListView listView = findViewById(R.id.recents_list);
        searchView = findViewById(R.id.search_recents);
        fromDateButton = findViewById(R.id.from_date_button);
        toDateButton = findViewById(R.id.to_date_button);
        adapter = new RecentCallAdapter(this, items);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> showRecentTapActions(items.get(position)));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showRecentActions(items.get(position));
            return true;
        });
        searchView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { applyFilters(); }
        });
        fromDateButton.setOnClickListener(v -> pickDate(true));
        toDateButton.setOnClickListener(v -> pickDate(false));
        findViewById(R.id.clear_date_filter_button).setOnClickListener(v -> {
            fromDateMillis = null;
            toDateMillis = null;
            updateDateButtons();
            applyFilters();
        });
        updateDateButtons();
        loadRecents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecents();
    }

    private void pickDate(boolean from) {
        Calendar calendar = Calendar.getInstance();
        long base = from ? (fromDateMillis == null ? System.currentTimeMillis() : fromDateMillis) : (toDateMillis == null ? System.currentTimeMillis() : toDateMillis);
        calendar.setTimeInMillis(base);
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(Calendar.YEAR, year);
            picked.set(Calendar.MONTH, month);
            picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            picked.set(Calendar.HOUR_OF_DAY, from ? 0 : 23);
            picked.set(Calendar.MINUTE, from ? 0 : 59);
            picked.set(Calendar.SECOND, from ? 0 : 59);
            picked.set(Calendar.MILLISECOND, from ? 0 : 999);
            if (from) {
                fromDateMillis = picked.getTimeInMillis();
            } else {
                toDateMillis = picked.getTimeInMillis();
            }
            updateDateButtons();
            applyFilters();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void updateDateButtons() {
        fromDateButton.setText(fromDateMillis == null ? getString(R.string.from_date) : DateFormat.format("yyyy-MM-dd", new Date(fromDateMillis)));
        toDateButton.setText(toDateMillis == null ? getString(R.string.to_date) : DateFormat.format("yyyy-MM-dd", new Date(toDateMillis)));
    }

    private void showRecentActions(RecentCallItem item) {
        boolean blocked = BlockedNumberStore.isBlocked(this, item.number);
        String[] actions = new String[]{getString(R.string.call), getString(R.string.copy_number), blocked ? getString(R.string.unblock_number) : getString(R.string.block_number)};
        new AlertDialog.Builder(this)
                .setTitle(item.name)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        dialNumber(item.number);
                    } else if (which == 1) {
                        shareToClipboard(getString(R.string.copy_number), item.number);
                        Toast.makeText(this, R.string.copy_number, Toast.LENGTH_SHORT).show();
                    } else {
                        BlockedNumberStore.setBlocked(this, item.number, !blocked);
                        Toast.makeText(this, blocked ? R.string.unblock_number : R.string.block_number, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showRecentTapActions(RecentCallItem item) {
        String[] actions = new String[]{
                getString(R.string.call),
                getString(R.string.view_details),
                getString(R.string.add_to_contacts),
                getString(R.string.contact_analysis)
        };
        new AlertDialog.Builder(this)
                .setTitle(item.name)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        dialNumber(item.number);
                    } else if (which == 1) {
                        showRecentDetails(item);
                    } else if (which == 2) {
                        addRecentNumberToContacts(item);
                    } else {
                        showRecentAnalysis(item);
                    }
                })
                .show();
    }

    private void showRecentDetails(RecentCallItem item) {
        StringBuilder message = new StringBuilder()
                .append(item.number)
                .append("\n\n")
                .append(item.details);
        String telephonyInfo = TelephonyInfoCollector.collect(this);
        if (!telephonyInfo.isEmpty()) {
            message.append("\n\n")
                    .append(getString(R.string.current_network_info))
                    .append("\n")
                    .append(telephonyInfo);
        }
        new AlertDialog.Builder(this)
                .setTitle(item.name)
                .setMessage(message.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void addRecentNumberToContacts(RecentCallItem item) {
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.NAME, item.name.equals(item.number) ? "" : item.name);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, item.number);
        startActivity(intent);
    }

    private void showRecentAnalysis(RecentCallItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.contact_analysis)
                .setMessage(ContactAnalysisUtil.buildAnalysis(this, item.name, item.number))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void loadRecents() {
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, REQ_CALL_LOG);
            return;
        }
        allItems.clear();
        try (Cursor cursor = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.DURATION},
                null,
                null,
                CallLog.Calls.DATE + " DESC")) {
            if (cursor != null) {
                int count = 0;
                while (cursor.moveToNext() && count < 100) {
                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    long date = cursor.getLong(2);
                    int type = cursor.getInt(3);
                    long duration = cursor.getLong(4);
                    String detail = DateFormat.format("yyyy-MM-dd HH:mm", new Date(date)) + "  type=" + type + "  duration=" + duration + "s";
                    allItems.add(new RecentCallItem(name == null || name.isEmpty() ? number : name, number, detail, date));
                    count++;
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage() == null ? getString(R.string.failed_to_load_recents) : e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        allItems.addAll(ImportedCallHistoryStore.loadItems(this));
        Collections.sort(allItems, Comparator.comparingLong(item -> -item.timestamp));
        applyFilters();
    }

    private void applyFilters() {
        String query = searchView == null ? "" : String.valueOf(searchView.getText()).trim().toLowerCase(Locale.US);
        items.clear();
        for (RecentCallItem item : allItems) {
            if (!query.isEmpty()) {
                String haystack = (item.name + " " + item.number + " " + item.details).toLowerCase(Locale.US);
                if (!haystack.contains(query)) {
                    continue;
                }
            }
            if (fromDateMillis != null && item.timestamp < fromDateMillis) {
                continue;
            }
            if (toDateMillis != null && item.timestamp > toDateMillis) {
                continue;
            }
            items.add(item);
        }
        adapter.notifyDataSetChanged();
        findViewById(R.id.empty_recents).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CALL_LOG) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadRecents();
            } else {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
            }
        }
    }
}

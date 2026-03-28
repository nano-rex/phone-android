package org.convoy.phone.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
import android.text.format.DateFormat;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.convoy.phone.R;
import org.convoy.phone.model.RecentCallItem;
import org.convoy.phone.util.BaseActivity;
import org.convoy.phone.util.BlockedNumberStore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RecentsActivity extends BaseActivity {
    private static final int REQ_CALL_LOG = 3;
    private final List<RecentCallItem> items = new ArrayList<>();
    private ArrayAdapter<RecentCallItem> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recents);
        bindBottomNav(R.id.tab_recents);
        ListView listView = findViewById(R.id.recents_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> dialNumber(items.get(position).number));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showRecentActions(items.get(position));
            return true;
        });
        loadRecents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecents();
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

    private void loadRecents() {
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, REQ_CALL_LOG);
            return;
        }
        items.clear();
        try (Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.DURATION},
                null, null, CallLog.Calls.DATE + " DESC LIMIT 100")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    long date = cursor.getLong(2);
                    int type = cursor.getInt(3);
                    long duration = cursor.getLong(4);
                    String detail = DateFormat.format("yyyy-MM-dd HH:mm", new Date(date)) + "  type=" + type + "  duration=" + duration + "s";
                    items.add(new RecentCallItem(name == null || name.isEmpty() ? number : name, number, detail));
                }
            }
        }
        adapter.notifyDataSetChanged();
        findViewById(R.id.empty_recents).setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}

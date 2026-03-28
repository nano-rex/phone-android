package org.convoy.phone.activities;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.convoy.phone.R;
import org.convoy.phone.model.RecordingItem;
import org.convoy.phone.util.AppSettings;
import org.convoy.phone.util.BaseActivity;
import org.convoy.phone.util.StorageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecordingsActivity extends BaseActivity {
    private final List<RecordingItem> items = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private RecordingItem current;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ArrayAdapter<RecordingItem> adapter;
    private SeekBar seekBar;
    private TextView playbackStatus;
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                playbackStatus.setText(formatTime(mediaPlayer.getCurrentPosition()) + " / " + formatTime(mediaPlayer.getDuration()));
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);
        bindBottomNav(R.id.tab_recordings);
        seekBar = findViewById(R.id.recordings_seek);
        playbackStatus = findViewById(R.id.recordings_status);
        ListView listView = findViewById(R.id.recordings_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> play(items.get(position)));
        Button playPause = findViewById(R.id.play_pause_button);
        playPause.setOnClickListener(v -> toggle());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        loadRecordings();
    }

    private void loadRecordings() {
        if (AppSettings.getRecordingsTreeUri(this) == null) {
            findViewById(R.id.recordings_missing_folder).setVisibility(android.view.View.VISIBLE);
            return;
        }
        items.clear();
        items.addAll(StorageUtil.listRecordings(this));
        adapter.notifyDataSetChanged();
        findViewById(R.id.recordings_empty).setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void play(RecordingItem item) {
        releasePlayer();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, item.uri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            current = item;
            seekBar.setMax(mediaPlayer.getDuration());
            playbackStatus.setText(item.name + "\n" + formatTime(0) + " / " + formatTime(mediaPlayer.getDuration()));
            handler.post(progressRunnable);
            mediaPlayer.setOnCompletionListener(mp -> releasePlayer());
        } catch (Exception e) {
            releasePlayer();
        }
    }

    private void toggle() {
        if (mediaPlayer == null && !items.isEmpty()) {
            play(items.get(0));
            return;
        }
        if (mediaPlayer == null) {
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
            handler.post(progressRunnable);
        }
    }

    private String formatTime(int ms) {
        int total = ms / 1000;
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60);
    }

    private void releasePlayer() {
        handler.removeCallbacks(progressRunnable);
        if (mediaPlayer != null) {
            try { mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        current = null;
        seekBar.setProgress(0);
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}

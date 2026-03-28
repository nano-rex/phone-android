package org.convoy.phone.activities

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.documentfile.provider.DocumentFile
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.convoy.phone.R
import org.convoy.phone.databinding.ActivityRecordingsBinding
import org.convoy.phone.extensions.config

class RecordingsActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityRecordingsBinding::inflate)
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var selectedUri: Uri? = null
    private var selectedName = ""
    private var seeking = false

    private val progressTicker = object : Runnable {
        override fun run() {
            val player = mediaPlayer ?: return
            if (!seeking) {
                binding.recordingsSeekbar.max = player.duration.coerceAtLeast(0)
                binding.recordingsSeekbar.progress = player.currentPosition.coerceAtLeast(0)
                binding.recordingsPosition.text = getString(
                    R.string.time_pair,
                    formatTime(player.currentPosition / 1000),
                    formatTime(player.duration / 1000)
                )
            }
            handler.postDelayed(this, 250L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge()
        setupTopAppBar(binding.recordingsAppbar, NavigationIcon.Arrow)
        setupPlayerControls()
        loadRecordings()
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(binding.recordingsHolder)
        loadRecordings()
    }

    override fun onPause() {
        super.onPause()
        stopPlayback()
    }

    private fun setupPlayerControls() = binding.apply {
        recordingsPlayPause.setOnClickListener {
            when {
                mediaPlayer == null && selectedUri != null -> startPlayback(selectedUri!!, selectedName)
                mediaPlayer?.isPlaying == true -> pausePlayback()
                mediaPlayer != null -> resumePlayback()
            }
        }

        recordingsSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    recordingsPosition.text = getString(
                        R.string.time_pair,
                        formatTime(progress / 1000),
                        formatTime((mediaPlayer?.duration ?: 0) / 1000)
                    )
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                seeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mediaPlayer?.seekTo(seekBar?.progress ?: 0)
                seeking = false
            }
        })
    }

    private fun loadRecordings() {
        val treeUri = config.callRecordingFolderUri
        val tree = if (treeUri.isBlank()) null else DocumentFile.fromTreeUri(this, Uri.parse(treeUri))
        val files = tree?.listFiles()
            ?.filter { it.isFile && (it.name?.endsWith(".wav", true) == true || it.name?.endsWith(".mp3", true) == true || it.type?.startsWith("audio/") == true) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

        binding.recordingsEmpty.text = if (treeUri.isBlank()) getString(R.string.set_recordings_folder_first) else getString(R.string.no_recordings_found)
        binding.recordingsEmpty.beVisibleIf(files.isEmpty())
        binding.recordingsList.beGoneIf(files.isEmpty())
        binding.recordingsPlayerHolder.beVisibleIf(files.isNotEmpty())

        binding.recordingsList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, files.map { it.name ?: getString(R.string.recordings) })
        binding.recordingsList.setOnItemClickListener { _, _, position, _ ->
            val file = files[position]
            selectedUri = file.uri
            selectedName = file.name ?: getString(R.string.recordings)
            startPlayback(file.uri, selectedName)
        }
    }

    private fun startPlayback(uri: Uri, name: String) {
        stopPlayback()
        selectedUri = uri
        selectedName = name
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@RecordingsActivity, uri)
            setOnPreparedListener {
                it.start()
                binding.recordingsNowPlaying.text = getString(R.string.now_playing_value, name)
                binding.recordingsPlayPause.text = getString(R.string.pause_recording_playback)
                handler.post(progressTicker)
            }
            setOnCompletionListener {
                binding.recordingsPlayPause.text = getString(R.string.play_recording_playback)
                binding.recordingsSeekbar.progress = 0
                binding.recordingsPosition.text = getString(R.string.time_zero_pair)
                handler.removeCallbacks(progressTicker)
            }
            prepare()
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        binding.recordingsPlayPause.text = getString(R.string.play_recording_playback)
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        binding.recordingsPlayPause.text = getString(R.string.pause_recording_playback)
        handler.post(progressTicker)
    }

    private fun stopPlayback() {
        handler.removeCallbacks(progressTicker)
        mediaPlayer?.runCatching {
            stop()
            release()
        }
        mediaPlayer = null
        binding.recordingsPlayPause.text = getString(R.string.play_recording_playback)
    }

    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}

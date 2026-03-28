package org.convoy.phone.helpers

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.documentfile.provider.DocumentFile
import org.convoy.phone.extensions.config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object CallRecorder {
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    @Volatile
    private var session: Session? = null

    fun maybeStart(context: Context) {
        if (session != null) return
        val config = Config.newInstance(context)
        if (!config.callRecordingEnabled || config.callRecordingFolderUri.isBlank()) return
        val session = buildSession(context, config.callRecordingSource) ?: return
        this.session = session
        session.worker.start()
    }

    fun maybeStop(context: Context) {
        val current = session ?: return
        session = null

        try {
            current.running.set(false)
            current.worker.join()

            val outputUri = createOutputUri(context) ?: return
            context.contentResolver.openOutputStream(outputUri, "w")?.use { out ->
                writeWaveFile(out, current.tempPcmFile)
            }
        } catch (_: Exception) {
        } finally {
            try {
                current.recorder.release()
            } catch (_: Exception) {
            }
            current.tempPcmFile.delete()
        }
    }

    private fun buildSession(context: Context, preferredSource: String): Session? {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBuffer, 4096)
        val sources = if (preferredSource == RECORDING_SOURCE_DEVICE) {
            listOf(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.MIC
            )
        } else {
            listOf(
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.VOICE_COMMUNICATION
            )
        }

        var recorder: AudioRecord? = null
        for (source in sources) {
            val candidate = AudioRecord(source, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
            if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                recorder = candidate
                break
            }
            candidate.release()
        }
        recorder ?: return null

        val tempFile = File.createTempFile("call-recording-", ".pcm", context.cacheDir)
        val running = AtomicBoolean(true)
        val worker = Thread({
            FileOutputStream(tempFile).use { fileOut ->
                val buffer = ByteArray(bufferSize)
                recorder.startRecording()
                while (running.get()) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) fileOut.write(buffer, 0, read)
                }
                try {
                    recorder.stop()
                } catch (_: Exception) {
                }
            }
        }, "call-recorder")

        return Session(recorder, tempFile, running, worker)
    }

    private fun createOutputUri(context: Context): android.net.Uri? {
        val tree = DocumentFile.fromTreeUri(context, android.net.Uri.parse(context.config.callRecordingFolderUri)) ?: return null
        val name = "call_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.wav"
        return tree.createFile("audio/wav", name)?.uri
    }

    private fun writeWaveFile(out: OutputStream, tempPcmFile: File) {
        val enhancedBytes = enhancePcm16(FileInputStream(tempPcmFile).use { it.readBytes() })
        val dataLength = enhancedBytes.size
        val header = ByteArrayOutputStream(44)
        val byteRate = SAMPLE_RATE * 2

        header.write("RIFF".toByteArray())
        header.write(intToLittleEndian(dataLength + 36))
        header.write("WAVE".toByteArray())
        header.write("fmt ".toByteArray())
        header.write(intToLittleEndian(16))
        header.write(shortToLittleEndian(1))
        header.write(shortToLittleEndian(1))
        header.write(intToLittleEndian(SAMPLE_RATE))
        header.write(intToLittleEndian(byteRate))
        header.write(shortToLittleEndian(2))
        header.write(shortToLittleEndian(16))
        header.write("data".toByteArray())
        header.write(intToLittleEndian(dataLength))

        out.write(header.toByteArray())
        out.write(enhancedBytes)
    }

    private fun enhancePcm16(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) return bytes
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val samples = ShortArray(bytes.size / 2)
        for (i in samples.indices) {
            samples[i] = bb.getShort()
        }

        val floats = FloatArray(samples.size)
        for (i in samples.indices) {
            floats[i] = samples[i] / 32768f
        }

        var previousIn = 0f
        var previousOut = 0f
        for (i in floats.indices) {
            val current = floats[i]
            val filtered = 0.97f * (previousOut + current - previousIn)
            floats[i] = filtered
            previousIn = current
            previousOut = filtered
        }

        val out = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in floats) {
            val shaped = kotlin.math.tanh((sample * 1.5f).toDouble()).toFloat() / 1.05f
            out.putShort((shaped.coerceIn(-0.98f, 0.98f) * 32767f).toInt().toShort())
        }
        return out.array()
    }

    private fun intToLittleEndian(value: Int) = byteArrayOf(
        (value and 0xff).toByte(),
        (value shr 8 and 0xff).toByte(),
        (value shr 16 and 0xff).toByte(),
        (value shr 24 and 0xff).toByte()
    )

    private fun shortToLittleEndian(value: Int) = byteArrayOf(
        (value and 0xff).toByte(),
        (value shr 8 and 0xff).toByte()
    )

    private data class Session(
        val recorder: AudioRecord,
        val tempPcmFile: File,
        val running: AtomicBoolean,
        val worker: Thread
    )
}

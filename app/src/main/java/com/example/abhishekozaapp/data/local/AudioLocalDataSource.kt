package com.example.abhishekozaapp.data.local

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class AudioLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var outputFile: String? = null
    private var isRecording = false
    private var isPlaying = false

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    /**
     * Starts recording safely. Creates /recordings directory if needed.
     */
    fun startRecording(): String? {
        if (isRecording) return outputFile

        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        outputFile = "${dir.absolutePath}/recording_${System.currentTimeMillis()}.m4a"

        try {
            requestAudioFocus() // ✅ Request focus before starting

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            outputFile = null
            abandonAudioFocus() // ✅ Release if failed
        }

        return outputFile
    }


    /**
     * Stops current recording safely and releases resources.
     */
    fun stopRecording(): String? {
        if (!isRecording) return null

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isRecording = false
            recorder = null
            abandonAudioFocus()
        }

        return outputFile
    }

    /**
     * Plays an audio file safely. Stops existing playback first.
     */
    fun playAudio(
        filePath: String,
        onStart: (() -> Unit)? = null,
        onCompletion: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        stopRecordingSafely()
        stopAudioIfPlaying()

        try {
            val focusGranted = requestAudioFocus()
            if (!focusGranted) {
                onError?.invoke("Unable to get audio focus")
                return
            }

            player = MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setDataSource(filePath)
                prepare()
                start()

                onStart?.invoke()

                setOnCompletionListener {
                    stopAudio()
                    onCompletion?.invoke()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke(e.localizedMessage ?: "Playback failed")
            stopAudio()
        }
    }




    /**
     * Stops playback safely.
     */
    fun stopAudio() {
        try {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            player = null
            isPlaying = false
            abandonAudioFocus()
        }
    }

    /**
     * Returns list of all recorded files.
     */
    fun getAllRecordings(): List<File> {
        val dir = File(context.filesDir, "recordings")
        return dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Checks whether recording is ongoing.
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Checks whether playback is ongoing.
     */
    fun isPlaying(): Boolean = isPlaying

    /**
     * Release recorder safely
     */
    private fun releaseRecorder() {
        try {
            recorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
        isRecording = false
    }

    // ————————————————————————————————————————————
    // 🔊 Audio Focus Helpers
    // ————————————————————————————————————————————
    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> stopAudio()
                    }
                }
                .setWillPauseWhenDucked(true) // ✅ ensures background apps pause, not duck
                .build()

            audioManager.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    ) {
                        stopAudio()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }


    fun stopRecordingSafely() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) { /* ignore stop errors */ }
        recorder = null
    }

    fun stopAudioIfPlaying() {
        try {
            player?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) { /* ignore */ }
        player = null
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            audioManager.abandonAudioFocus(null)
        }
        focusRequest = null
    }
}

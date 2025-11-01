package com.example.abhishekozaapp.core

import android.content.Context
import android.media.MediaRecorder
import com.example.abhishekozaapp.core.AppUtil.appToast
import java.io.File

/**
 * Description : Audio Recording Methods & It's behaviour setup
 * @author Abhishek Oza
 */
object AudioRecorder {

    private var mRecorder: MediaRecorder? = null
    private var recordedFile: String? = null

    // Start Recording
    fun startAudioRecording(context: Context, onStart: () -> Unit = {}): String? {
        return try {
            val dir = context.getExternalFilesDir("recordings")
            if (dir != null && !dir.exists()) dir.mkdirs()

            val file = File(dir, "rec_${System.currentTimeMillis()}.mp4")
            recordedFile = file.absolutePath

            mRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordedFile)
                prepare()
                start()
            }

            appToast(context, "Recording start")
            onStart.invoke() // invoke callback
            recordedFile
        } catch (e: Exception) {
            e.printStackTrace()
            appToast(context, "Failed for start recording")
            null
        }
    }

    // Stop Recording
    fun stopAudioRecording(context: Context, onStop: () -> Unit = {}): String? {
        return try {
            mRecorder?.apply {
                stop()
                release()
            }
            mRecorder = null
            onStop.invoke()
            appToast(context, "Saved successfully")
            recordedFile
        } catch (e: Exception) {
            e.printStackTrace()
            appToast(context, "Fail to stop recording")
            null
        }
    }

    // Get last recorded file
    fun getLastAudioRecordedFile(): String? = recordedFile

    // safely release
    fun releaseRecorder() {
        try {
            mRecorder?.release()
            mRecorder = null
        } catch (_: Exception) {
        }
    }

}
package com.example.abhishekozaapp.core

import android.content.Context
import android.media.AudioManager

/**
 * Description : Audio Focus Activity
 * @author Abhishek Oza
 */
object AudioFocus {

    private var aManager: AudioManager? = null
    private var channelListener: AudioManager.OnAudioFocusChangeListener? = null

    // For Pause Other APP Audio Activity
    fun requestToFocus(context: Context): Boolean {
        aManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        channelListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {}
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {}
            }
        }

        val result = aManager?.requestAudioFocus(
            channelListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    // Other App can resume there audio activity
    fun abandonAudioFocus() {
        aManager?.abandonAudioFocus(channelListener)
        channelListener = null
        aManager = null
    }
}
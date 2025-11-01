package com.example.abhishekozaapp.presentation.activity

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.abhishekozaapp.R
import com.example.abhishekozaapp.core.AppUtil.appToast
import com.example.abhishekozaapp.core.AudioFocus
import com.example.abhishekozaapp.core.AudioRecorder
import com.example.abhishekozaapp.core.DayNight
import com.example.abhishekozaapp.core.RaiseToEar
import com.example.abhishekozaapp.core.RunTimePermission
import com.example.abhishekozaapp.databinding.ActivityMainBinding
import com.example.abhishekozaapp.presentation.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    lateinit var activityMainBinding: ActivityMainBinding

    private val mainViewModel: MainViewModel by viewModels()

    private var recordedFile: String? = null
    private var mediaPlayer: MediaPlayer? = null

    private var raiseToEar: RaiseToEar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        DayNight.setUp()
        setUpRaiseToEar() // setUp Proximity sensor
        setUpBinding() // set up binding
    }

    override fun onResume() {
        super.onResume()
        DayNight.setUp()
    }

    private fun routePlayback(useEarpiece: Boolean) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Switch mode based on proximity
        audioManager.mode = if (useEarpiece) {
            AudioManager.MODE_IN_COMMUNICATION
        } else {
            AudioManager.MODE_NORMAL
        }

        audioManager.isSpeakerphoneOn = !useEarpiece

        // Re-create player with new routing
        mediaPlayer?.let { player ->
            val position = player.currentPosition
            val path = recordedFile ?: return
            player.stop()
            player.release()

            mediaPlayer = MediaPlayer().apply {
                setAudioStreamType(
                    if (useEarpiece) AudioManager.STREAM_VOICE_CALL
                    else AudioManager.STREAM_MUSIC
                )
                setDataSource(path)
                prepare()
                seekTo(position)
                start()
            }
        }
    }


    /*
     * set up binding
     */
    private fun setUpBinding() {
        activityMainBinding.apply {
            recordBtn.setOnClickListener { runTimePermission() }
            playBtn.setOnClickListener {
                val isRecording = mainViewModel.isRecordingStart.value
                if (isRecording) {
                    stopRecording()
                    recordedFile?.let { file ->
                        playBtn.postDelayed({
                            playAudioRecording(file)
                        }, 500)
                    } ?: run {
                        appToast(this@MainActivity, "No recording found")
                    }
                } else {
                    recordedFile?.let {
                        playAudioRecording(it)
                    } ?: run {
                        appToast(this@MainActivity, "Please record to play")
                    }
                }
            }
        }
    }

    private fun setUpRaiseToEar() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        raiseToEar = RaiseToEar(
            this,
            audioManager,
            onNear = {
                appToast(this, "Earpiece mode")
                routePlayback(true)
            },
            onFar = {
                appToast(this, "Speaker mode")
                routePlayback(false)
            }
        )
    }


    /*
     * Get run time permission
     */
    private fun runTimePermission() {
        if (RunTimePermission.checkingPermission(this)) {
            startStopRecord()
        } else {
            RunTimePermission.requestAppPermissions(this)
        }
    }

    /*
     * Toggle between start and stop recording
     */
    private fun startStopRecord() {
        val isRecording = mainViewModel.isRecordingStart.value
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    /*
     * start recording
     */
    private fun startRecording() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
                player.release()
                mediaPlayer = null
                appToast(this, "Playback stopped to start recording")
            }
        }

        // For pause other app's activity
        AudioFocus.requestToFocus(this)

        recordedFile = AudioRecorder.startAudioRecording(this) {
            mainViewModel.isRecordingStart.value = true
            activityMainBinding.recordBtn.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_mic_stop
                )
            )
        }
    }

    /*
    * Stop recording
    */
    private fun stopRecording() {
        recordedFile = AudioRecorder.stopAudioRecording(this) {
            mainViewModel.isRecordingStart.value = false
            activityMainBinding.recordBtn.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_mic_rec
                )
            )

            // To resume other app's activity
            AudioFocus.abandonAudioFocus()
        }
    }

    /*
     * Play recording
     */
    private fun playAudioRecording(path: String) {
        val focusGranted = AudioFocus.requestToFocus(this)
        if (!focusGranted) {
            appToast(this, "Unable to get audio focus")
            return
        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        // Start with loudspeaker mode
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = true

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setDataSource(path)
            prepare()
            start()
        }

        appToast(this, "Playing Audio")
        raiseToEar?.register()

        mediaPlayer?.setOnCompletionListener {
            it.release()
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = true
            AudioFocus.abandonAudioFocus()
            raiseToEar?.unregister()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        AudioFocus.abandonAudioFocus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        RunTimePermission.manageUserActions(
            requestCode,
            grantResults,
            onGranted = { appToast(this, "Please Start Recording") },
            onDenied = {
                appToast(this, "Permissions required to record audio")
            }
        )
    }

}
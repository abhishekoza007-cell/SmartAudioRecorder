package com.example.abhishekozaapp.presentation.ui.activity

import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.abhishekozaapp.R
import com.example.abhishekozaapp.core.AppUtil.appToast
import com.example.abhishekozaapp.core.AudioFocus
import com.example.abhishekozaapp.core.RaiseToEar
import com.example.abhishekozaapp.core.RunTimePermission
import com.example.abhishekozaapp.data.handler.ThemeManager
import com.example.abhishekozaapp.databinding.ActivityMainBinding
import com.example.abhishekozaapp.presentation.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var audioManager: AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var recordedFile: String? = null
    private var raiseToEar: RaiseToEar? = null

    @Inject
    lateinit var themeManager: ThemeManager

    @SuppressLint("ScheduleExactAlarm")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkThemeUpdate()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        themeManager.applyThemeNow()
        themeManager.scheduleAutoSwitch()

        setupListeners()
        observeViewModel()
        setupRaiseToEar()
    }

    private fun setupListeners() {
        binding.apply {
            recordBtn.setOnClickListener {
                if (RunTimePermission.checkingPermission(this@MainActivity)) {
                    if (viewModel.isRecording.value) stopRecording()
                    else startRecording()
                } else {
                    RunTimePermission.requestAppPermissions(this@MainActivity)
                }
            }

            playBtn.setOnClickListener {
                val recordings = viewModel.recordings.value
                if (recordings.isEmpty()) {
                    appToast(this@MainActivity, "Please record something first")
                    return@setOnClickListener
                }
                val latestFile = recordings.first().absolutePath
                playAudioRecording(latestFile)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isRecording.collectLatest { isRecording ->
                binding.recordBtn.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@MainActivity,
                        if (isRecording) R.drawable.ic_mic_stop else R.drawable.ic_mic_rec
                    )
                )
            }
        }

        lifecycleScope.launch {
            viewModel.isPlaying.collectLatest { isPlaying ->
                binding.playBtn.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@MainActivity,
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    )
                )
            }
        }
    }

    private fun setupRaiseToEar() {
        raiseToEar = RaiseToEar(
            this,
            audioManager,
            onNear = {
                routePlayback(true)
                appToast(this, "Earpiece mode")
            },
            onFar = {
                routePlayback(false)
                appToast(this, "Speaker mode")
            }
        )
    }

    private fun startRecording() {
        try {
            // 🛑 Stop playback before recording
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                    player.release()
                    mediaPlayer = null
                    appToast(this, "Playback stopped to start recording")
                }
            }

            raiseToEar?.unregister()
            AudioFocus.requestToFocus(this)
            viewModel.startRecording()
            appToast(this, "Recording started")
        } catch (e: Exception) {
            e.printStackTrace()
            appToast(this, "Failed to start recording")
        }
    }

    private fun stopRecording() {
        viewModel.stopRecording()
        AudioFocus.abandonAudioFocus()
        appToast(this, "Recording stopped")
        raiseToEar?.unregister()
    }

    private fun playAudioRecording(path: String) {
        if (viewModel.isRecording.value) {
            stopRecording()
            appToast(this, "Recording stopped — preparing playback...")
            binding.playBtn.postDelayed({
                playAudioRecording(path)
            }, 600)
            return
        }

        val focusGranted = AudioFocus.requestToFocus(this)
        if (!focusGranted) {
            appToast(this, "Unable to get audio focus")
            return
        }

        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = true

        // Stop any existing playback
        mediaPlayer?.runCatching {
            stop()
            release()
        }
        mediaPlayer = null
        recordedFile = path

        try {
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
                mediaPlayer = null
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.isSpeakerphoneOn = true
                AudioFocus.abandonAudioFocus()
                raiseToEar?.unregister()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            appToast(this, "Playback failed: ${e.message}")
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun routePlayback(useEarpiece: Boolean) {
        audioManager.mode = if (useEarpiece) {
            AudioManager.MODE_IN_COMMUNICATION
        } else {
            AudioManager.MODE_NORMAL
        }
        audioManager.isSpeakerphoneOn = !useEarpiece

        // Recreate player with correct routing
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

    override fun onResume() {
        super.onResume()
        themeManager.recheckTheme(this)
    }

    override fun onPause() {
        super.onPause()
        themeManager.recheckTheme(this)
        raiseToEar?.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        raiseToEar?.unregister()
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
            onDenied = { appToast(this, "Permissions required to record audio") }
        )
    }

    private fun checkThemeUpdate() {
        val themeRes = if (themeManager.isNightTime()) {
            R.style.AppTheme_Night
        } else {
            R.style.AppTheme_Day
        }
        setTheme(themeRes)
    }
}

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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var raiseToEar: RaiseToEar? = null
    private var mediaPlayer: MediaPlayer? = null

    @Inject
    lateinit var themeManager: ThemeManager

    @SuppressLint("ScheduleExactAlarm")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkThemeUpdate()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                lifecycleScope.launch {
                    if (viewModel.isRecording.value) {
                        viewModel.stopRecording()
                        viewModel.isRecording.filter { !it }.first()
                    }
                    mediaPlayer?.let {
                        it.stop()
                        it.release()
                        mediaPlayer = null
                    }

                    // Get latest recorded file
                    val latestFile = viewModel.recordings.value.firstOrNull()
                    if (latestFile == null) {
                        appToast(this@MainActivity, "No recordings found")
                        return@launch
                    }

                    playAudioRecording(latestFile.absolutePath)
                }
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
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
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

    private fun routePlayback(useEarpiece: Boolean) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.mode = AudioManager.MODE_NORMAL

        if (useEarpiece) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = false
        } else {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = true
        }

        mediaPlayer?.let { player ->
            val position = player.currentPosition
            val filePath = viewModel.recordings.value.firstOrNull()?.absolutePath ?: return
            player.stop()
            player.release()

            mediaPlayer = MediaPlayer().apply {
                setAudioStreamType(
                    if (useEarpiece) AudioManager.STREAM_VOICE_CALL
                    else AudioManager.STREAM_MUSIC
                )
                setDataSource(filePath)
                prepare()
                seekTo(position)
                start()
            }
        }
    }

    private fun startRecording() {
        try {

            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                appToast(this, "Playback stopped to start recording")
            }

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
        raiseToEar?.unregister()
        appToast(this, "Recording stopped")
    }

    private fun playAudioRecording(path: String) {
        val focusGranted = AudioFocus.requestToFocus(this)
        if (!focusGranted) {
            appToast(this, "Unable to get audio focus")
            return
        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = true

        try {
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
                mediaPlayer = null
                raiseToEar?.unregister()
                AudioFocus.abandonAudioFocus()
                appToast(this, "Playback completed")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            appToast(this, "Playback failed: ${e.message}")
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    override fun onPause() {
        super.onPause()
        raiseToEar?.unregister()
        themeManager.recheckTheme(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
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
            onGranted = { appToast(this, "Please start recording") },
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

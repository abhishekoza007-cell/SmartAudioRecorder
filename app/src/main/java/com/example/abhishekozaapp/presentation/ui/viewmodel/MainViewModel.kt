package com.example.abhishekozaapp.presentation.ui.viewmodel

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abhishekozaapp.core.AppUtil.appToast
import com.example.abhishekozaapp.core.RaiseToEar
import com.example.abhishekozaapp.domain.usecases.AutoSwitchRecordingUseCase
import com.example.abhishekozaapp.domain.usecases.GetRecordingUseCase
import com.example.abhishekozaapp.domain.usecases.PlayRecordingUseCase
import com.example.abhishekozaapp.domain.usecases.StartRecordingUseCase
import com.example.abhishekozaapp.domain.usecases.StopRecordingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val startRecording: StartRecordingUseCase,
    private val stopRecording: StopRecordingUseCase,
    private val getAllRecordings: GetRecordingUseCase,
    private val autoSwitchRecordingUseCase: AutoSwitchRecordingUseCase,
    private val playRecordingUseCase: PlayRecordingUseCase
) : ViewModel() {



    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _recordings = MutableStateFlow<List<File>>(emptyList())
    val recordings = _recordings.asStateFlow()

    private var currentFile: String? = null

    fun startRecording() {
        viewModelScope.launch {
            currentFile = startRecording.execute()
            _isRecording.value = true
            _isPlaying.value = false
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            currentFile = stopRecording.execute()
            _isRecording.value = false
            _recordings.value = getAllRecordings.execute()
        }
    }

    suspend fun playRecording(
        filePath: String,
        onStart: (() -> Unit)? = null,
        onCompletion: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        stopRecording()
        _isPlaying.value = true
        playRecordingUseCase.execute(
            filePath,
            onStart = {
                onStart?.invoke()
            },
            onCompletion = {
                _isPlaying.value = false
                onCompletion?.invoke()
            },
            onError = {
                _isPlaying.value = false
                onError?.invoke(it)
            }
        )
    }

    fun scheduleThemeSwitch() {
        autoSwitchRecordingUseCase.execute()
    }

}
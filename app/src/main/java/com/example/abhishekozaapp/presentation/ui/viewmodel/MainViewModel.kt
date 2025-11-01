package com.example.abhishekozaapp.presentation.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val startRecording: StartRecordingUseCase,
    private val stopRecording: StopRecordingUseCase,
    private val getAllRecordings: GetRecordingUseCase,
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
}
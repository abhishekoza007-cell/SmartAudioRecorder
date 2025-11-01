package com.example.abhishekozaapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel : ViewModel() {
     val isRecordingStart = MutableStateFlow(false)
}
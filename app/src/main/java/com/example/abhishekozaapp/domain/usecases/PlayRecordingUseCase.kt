package com.example.abhishekozaapp.domain.usecases

import com.example.abhishekozaapp.domain.repository.RecordingRepository
import javax.inject.Inject

class PlayRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend fun execute(
        filePath: String,
        onStart: (() -> Unit)? = null,
        onCompletion: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        repository.playAudio(filePath, onStart, onCompletion, onError)
    }
}
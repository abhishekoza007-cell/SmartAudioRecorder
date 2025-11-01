package com.example.abhishekozaapp.domain.usecases

import com.example.abhishekozaapp.domain.repository.RecordingRepository

class StartRecordingUseCase(private val recordingRepository: RecordingRepository) {
    suspend fun execute() = recordingRepository.startRecording()
}
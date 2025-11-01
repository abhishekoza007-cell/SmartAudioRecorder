package com.example.abhishekozaapp.domain.usecases

import com.example.abhishekozaapp.domain.repository.RecordingRepository

class StopRecordingUseCase(private val recordingRepository: RecordingRepository) {
    suspend fun execute() = recordingRepository.stopRecording()
}
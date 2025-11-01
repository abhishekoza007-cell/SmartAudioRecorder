package com.example.abhishekozaapp.domain.usecases

import com.example.abhishekozaapp.domain.repository.RecordingRepository

class AutoSwitchRecordingUseCase(private val recordingRepository: RecordingRepository) {
    fun execute() = recordingRepository.scheduleAutoSwitch()
}
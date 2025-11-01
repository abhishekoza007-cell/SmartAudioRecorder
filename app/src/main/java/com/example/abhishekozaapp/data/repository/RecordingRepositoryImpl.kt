package com.example.abhishekozaapp.data.repository

import android.annotation.SuppressLint
import com.example.abhishekozaapp.data.handler.ThemeManager
import com.example.abhishekozaapp.data.local.AudioLocalDataSource
import com.example.abhishekozaapp.domain.repository.RecordingRepository
import javax.inject.Inject

class RecordingRepositoryImpl @Inject constructor(
    private val localDataSource: AudioLocalDataSource,
    private val themeManager: ThemeManager
) : RecordingRepository {

    override suspend fun startRecording() = localDataSource.startRecording()

    override suspend fun stopRecording() = localDataSource.stopRecording()

    override suspend fun getAllRecordings() = localDataSource.getAllRecordings()

    @SuppressLint("ScheduleExactAlarm")
    override fun scheduleAutoSwitch() { themeManager.scheduleAutoSwitch() }

    override suspend fun playAudio(
        filePath: String,
        onStart: (() -> Unit)?,
        onCompletion: (() -> Unit)?,
        onError: ((String) -> Unit)?
    ) {
        localDataSource.playAudio(filePath, onStart, onCompletion, onError)
    }
}

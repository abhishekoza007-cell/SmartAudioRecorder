package com.example.abhishekozaapp.domain.repository

import java.io.File

interface RecordingRepository {
    suspend fun startRecording(): String?
    suspend fun stopRecording() : String?
    suspend fun getAllRecordings(): List<File>
    fun scheduleAutoSwitch()
    suspend fun playAudio(
        filePath: String,
        onStart: (() -> Unit)?,
        onCompletion: (() -> Unit)?,
        onError: ((String) -> Unit)?
    )
}
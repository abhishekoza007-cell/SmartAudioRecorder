package com.example.abhishekozaapp.presentation.di

import android.content.Context
import com.example.abhishekozaapp.data.handler.ThemeManager
import com.example.abhishekozaapp.data.local.AudioLocalDataSource
import com.example.abhishekozaapp.data.repository.RecordingRepositoryImpl
import com.example.abhishekozaapp.domain.repository.RecordingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    @Singleton
    fun provideRecordingRepository(
        localDataSource: AudioLocalDataSource,
        @ApplicationContext context: Context
    ): RecordingRepository {
        return RecordingRepositoryImpl(
            localDataSource = localDataSource,
            themeManager = ThemeManager(context)
        )
    }

}
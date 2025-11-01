package com.example.abhishekozaapp.presentation.di

import com.example.abhishekozaapp.domain.repository.RecordingRepository
import com.example.abhishekozaapp.domain.usecases.AutoSwitchRecordingUseCase
import com.example.abhishekozaapp.domain.usecases.GetRecordingUseCase
import com.example.abhishekozaapp.domain.usecases.PlayRecordingUseCase
import com.example.abhishekozaapp.domain.usecases.StartRecordingUseCase
import com.example.abhishekozaapp.domain.usecases.StopRecordingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class UseCaseModule {

    @Provides
    fun provideStartRecordingUseCase(repository: RecordingRepository): StartRecordingUseCase {
        return StartRecordingUseCase(repository)
    }

    @Provides
    fun provideStopRecordingUseCase(repository: RecordingRepository): StopRecordingUseCase {
        return StopRecordingUseCase(repository)
    }

    @Provides
    fun provideGetRecordingUseCase(repository: RecordingRepository): GetRecordingUseCase {
        return GetRecordingUseCase(repository)
    }

    @Provides
    fun provideAutoSwitchRecordingUseCase(repository: RecordingRepository): AutoSwitchRecordingUseCase {
        return AutoSwitchRecordingUseCase(repository)
    }

    @Provides
    fun providePlayRecordingUseCase(repository: RecordingRepository): PlayRecordingUseCase {
        return PlayRecordingUseCase(repository)
    }

}
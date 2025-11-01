package com.example.abhishekozaapp.presentation.di

import android.content.Context
import com.example.abhishekozaapp.data.handler.ThemeManager
import com.example.abhishekozaapp.data.local.AudioLocalDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule { // ✅ must be object, not abstract

    @Provides
    @Singleton
    fun provideAudioLocalDataSource(
        @ApplicationContext context: Context
    ): AudioLocalDataSource = AudioLocalDataSource(context)

    @Provides
    @Singleton
    fun provideThemeManager(
        @ApplicationContext context: Context
    ): ThemeManager = ThemeManager(context)
}

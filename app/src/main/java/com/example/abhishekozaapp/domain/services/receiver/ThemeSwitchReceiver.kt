package com.example.abhishekozaapp.domain.services.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import com.example.abhishekozaapp.data.handler.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ThemeSwitchReceiver : BroadcastReceiver() {

    @Inject
    lateinit var themeManager: ThemeManager

    @SuppressLint("ScheduleExactAlarm")
    override fun onReceive(context: Context, intent: Intent?) {
        themeManager.applyThemeNow()
        themeManager.scheduleAutoSwitch()
    }
}

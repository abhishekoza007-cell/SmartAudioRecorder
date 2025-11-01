package com.example.abhishekozaapp.data.handler

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatDelegate
import com.example.abhishekozaapp.R
import com.example.abhishekozaapp.domain.services.receiver.ThemeSwitchReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject

class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Apply current theme instantly based on time.
     * (Light = 06:00–17:59, Dark = 18:00–05:59)
     */
    fun applyThemeNow() {
        val desiredMode = if (isNightTime()) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        // Apply only if different — avoids flicker
        if (AppCompatDelegate.getDefaultNightMode() != desiredMode) {
            AppCompatDelegate.setDefaultNightMode(desiredMode)
        }
    }

    /**
     * Schedule next automatic theme switch (06:00 or 18:00)
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleAutoSwitch() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextSwitchTime = calculateNextSwitchTime()

        val intent = Intent(context, ThemeSwitchReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextSwitchTime.timeInMillis,
                        pendingIntent
                    )
                } else {
                    val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextSwitchTime.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Calculate the next time (06:00 or 18:00) to switch.
     */
    private fun calculateNextSwitchTime(): Calendar {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)

        if (hour in 6 until 18) {
            // Currently day — next switch at 18:00 today
            next.set(Calendar.HOUR_OF_DAY, 18)
            next.set(Calendar.MINUTE, 0)
            next.set(Calendar.SECOND, 0)
        } else {
            // Currently night — next switch at 06:00 next valid day
            next.set(Calendar.HOUR_OF_DAY, 6)
            next.set(Calendar.MINUTE, 0)
            next.set(Calendar.SECOND, 0)

            if (!next.after(now)) {
                next.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return next
    }

    /**
     * Returns true if it's currently night (18:00–05:59)
     */
    fun isNightTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 18 || hour < 6
    }

    /**
     * Recheck theme without flicker (used onResume or time change)
     */
    fun recheckTheme(activity: Activity) {
        val shouldBeNight = isNightTime()
        val currentNightMode =
            activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isCurrentlyNight = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        if (shouldBeNight != isCurrentlyNight) {
            val themeRes = if (shouldBeNight) R.style.AppTheme_Night else R.style.AppTheme_Day
            activity.setTheme(themeRes)

            activity.window.decorView.apply {
                alpha = 0f
                animate().alpha(1f).setDuration(250).start()
            }
        }
    }
}

package com.example.abhishekozaapp.core

import androidx.appcompat.app.AppCompatDelegate
import java.util.Calendar

/**
 * Description : To manage dark/light theme accordingly daily timing
 * @author Abhishek Oza
 */
object DayNight {

    /**
     * Automatically applies Dark or Light mode based on time of day.
     * Dark mode → from 7 PM to 6 AM.
     * Light mode → from 6 AM to 7 PM.
     */
    fun setUp() {
        if (isNightTime()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /**
     * Returns true if current hour is in night range.
     */
    private fun isNightTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour < 6 || hour >= 19 // 7 PM - 6 AM = night
    }
}
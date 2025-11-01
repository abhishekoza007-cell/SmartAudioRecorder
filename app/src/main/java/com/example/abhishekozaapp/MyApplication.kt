package com.example.abhishekozaapp

import android.app.Application
import com.example.abhishekozaapp.core.DayNight

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DayNight.setUp()
    }
}
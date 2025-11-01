package com.example.abhishekozaapp.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

class RaiseToEar(
    private val context: Context,
    private val audioManager: AudioManager,
    private val onNear: (() -> Unit)? = null,
    private val onFar: (() -> Unit)? = null
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var isRegistered = false

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    fun register() {
        if (isRegistered || proximitySensor == null) return
        sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        isRegistered = true
    }

    fun unregister() {
        if (!isRegistered) return
        sensorManager?.unregisterListener(this)
        isRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val distance = event.values[0]
        val isNear = distance < (proximitySensor?.maximumRange ?: 0f) / 2f
        if (isNear) onNear?.invoke() else onFar?.invoke()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
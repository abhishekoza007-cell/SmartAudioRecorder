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
    context: Context,
    private val audioManager: AudioManager,
    private val onNear: () -> Unit,
    private val onFar: () -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var isRegistered = false
    private var lastNear = false

    fun register() {
        if (!isRegistered && proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
            isRegistered = true
        }
    }

    fun unregister() {
        if (isRegistered) {
            sensorManager.unregisterListener(this)
            isRegistered = false
        }
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        val distance = p0?.values?.get(0) ?: return
        val isNear = distance < (proximitySensor?.maximumRange ?: 0f)

        if (isNear == lastNear) return
        lastNear = isNear

        if (isNear) {
            if (!audioManager.isBluetoothA2dpOn && !audioManager.isWiredHeadsetOn) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
            }
            onNear()
        } else {

            Handler(Looper.getMainLooper()).postDelayed({
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.isSpeakerphoneOn = true
            }, 200)

            onFar()
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}
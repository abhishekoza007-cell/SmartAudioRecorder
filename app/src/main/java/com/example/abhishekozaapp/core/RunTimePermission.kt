package com.example.abhishekozaapp.core

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Description : Helping class for runtime permission
 * @author Abhishek Oza
 */
object RunTimePermission {

    const val REQUEST_CODE = 100

    // Request List
    fun requestList(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        return permissions.toTypedArray()
    }

    // Check
    fun checkingPermission(activity: Activity): Boolean {
        return requestList().all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Request
    fun requestAppPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(activity, requestList(), REQUEST_CODE)
    }

    // Handle Actions
    fun manageUserActions(
        requestCode: Int,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }

}
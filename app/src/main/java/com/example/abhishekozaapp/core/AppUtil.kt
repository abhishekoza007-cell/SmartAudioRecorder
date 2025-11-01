package com.example.abhishekozaapp.core

import android.content.Context
import android.widget.Toast

/**
 * Description : Common utilities
 * @author Abhishek Oza
 */
object AppUtil {

    /*
     * To make toast
     */
    fun appToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}
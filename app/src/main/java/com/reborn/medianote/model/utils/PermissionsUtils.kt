package com.reborn.medianote.model.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class PermissionsUtils {
    companion object {
        const val REQUEST_CODE_IMAGE = 1
        const val REQUEST_CODE_AUDIO = 2

        private fun hasPermission(ctx: Context, permission: String): Boolean {
            return ActivityCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
        }

        private fun askPermissions(activity: Activity, requests: Array<String>, requestCode: Int) {
            ActivityCompat.requestPermissions(activity, requests, requestCode)
        }

        fun hasImagePermission(ctx: Context): Boolean {
            return (hasPermission(ctx, Manifest.permission.INTERNET) && hasPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE))
        }

        fun askImagePermissions(activity: Activity) {
            askPermissions(activity, arrayOf(Manifest.permission.INTERNET, Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_IMAGE)
        }

        fun hasAudioPermission(ctx: Context) : Boolean {
            return hasPermission(ctx, Manifest.permission.RECORD_AUDIO)
        }

        fun askAudioPermission(activity: Activity) {
            askPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE_AUDIO)
        }
    }
}
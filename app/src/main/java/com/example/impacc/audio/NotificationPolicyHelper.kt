package com.example.impacc.audio

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings


class NotificationPolicyHelper(
    private val context: Context
) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager


    fun hasAccess(): Boolean {

        return notificationManager.isNotificationPolicyAccessGranted

    }


    fun openSettings() {

        val intent = Intent(
            Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
        )

        context.startActivity(intent)

    }
}
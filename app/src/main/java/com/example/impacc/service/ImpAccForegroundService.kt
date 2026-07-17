package com.example.impacc.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.impacc.R
import com.example.impacc.audio.MicrophoneMonitor
import com.example.impacc.audio.RingerModeController

class ImpAccForegroundService : Service() {

    private lateinit var microphoneMonitor: MicrophoneMonitor
    private lateinit var ringerModeController: RingerModeController
    private var monitoringStarted = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        ringerModeController = RingerModeController(this)

        microphoneMonitor = MicrophoneMonitor(
            this,
            ringerModeController
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        if (!monitoringStarted) {
            microphoneMonitor.start()
            monitoringStarted = true
        }

        return START_STICKY
    }

    override fun onDestroy() {
        if (monitoringStarted) {
            microphoneMonitor.stop()
            monitoringStarted = false
        }

        getSharedPreferences(
            PREFERENCES_NAME,
            MODE_PRIVATE
        ).edit()
            .putBoolean(KEY_NOTIFICATION_CONTROL_ENABLED, false)
            .apply()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ImpAcc microphone monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description =
                "Keeps microphone activity monitoring active in the background"
        }

        val manager = getSystemService(
            NotificationManager::class.java
        )

        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("ImpAcc")
            .setContentText("Monitoring microphone activity")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID =
            "impacc_microphone_channel"

        private const val NOTIFICATION_ID = 1001

        private const val PREFERENCES_NAME =
            "impacc_preferences"

        private const val KEY_NOTIFICATION_CONTROL_ENABLED =
            "notification_control_enabled"
    }
}
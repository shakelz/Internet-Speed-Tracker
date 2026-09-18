package com.netspeed.tracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.netspeed.tracker.data.db.AppDatabase

class NetSpeedApplication : Application() {

    companion object {
        const val CHANNEL_SPEED_ID = "channel_speed_indicator_v2"
        const val CHANNEL_ALERT_ID = "channel_speed_alerts"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        AppDatabase.getDatabase(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Delete legacy low-priority channel if exists
            try {
                notificationManager.deleteNotificationChannel("channel_speed_indicator")
            } catch (ignored: Exception) {}

            // 1. Silent persistent channel for live speed meter (DEFAULT importance ensures status bar visibility)
            val speedChannel = NotificationChannel(
                CHANNEL_SPEED_ID,
                getString(R.string.channel_speed_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.channel_speed_desc)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            // 2. High importance channel for data limit warning alerts
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_alerts_desc)
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(speedChannel, alertChannel))
        }
    }
}

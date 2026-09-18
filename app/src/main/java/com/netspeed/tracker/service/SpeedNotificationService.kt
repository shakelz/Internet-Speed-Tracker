package com.netspeed.tracker.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.netspeed.tracker.NetSpeedApplication
import com.netspeed.tracker.R
import com.netspeed.tracker.data.NetworkStatsHelper
import com.netspeed.tracker.data.db.AppDatabase
import com.netspeed.tracker.data.db.DailyUsageEntity
import com.netspeed.tracker.data.pref.PreferenceManager
import com.netspeed.tracker.receiver.ScreenReceiver
import com.netspeed.tracker.ui.MainActivity
import com.netspeed.tracker.utils.SpeedIconGenerator
import com.netspeed.tracker.utils.SpeedUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SpeedNotificationService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002

        // Live Speed data streams for UI
        private val _liveSpeedFlow = MutableStateFlow(SpeedData())
        val liveSpeedFlow: StateFlow<SpeedData> = _liveSpeedFlow.asStateFlow()

        var isRunning: Boolean = false
            private set
    }

    data class SpeedData(
        val downloadBytesPerSec: Long = 0L,
        val uploadBytesPerSec: Long = 0L,
        val todayMobileBytes: Long = 0L,
        val todayWifiBytes: Long = 0L,
        val networkName: String = "",
        val networkType: String = ""
    )

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var trackerJob: Job? = null

    private lateinit var prefManager: PreferenceManager
    private lateinit var networkStatsHelper: NetworkStatsHelper
    private lateinit var database: AppDatabase
    private var screenReceiver: ScreenReceiver? = null

    private var lastTotalRx: Long = 0L
    private var lastTotalTx: Long = 0L
    private var lastCheckTime: Long = 0L
    private var pollingIntervalMs: Long = 1000L

    override fun onCreate() {
        super.onCreate()
        prefManager = PreferenceManager(this)
        networkStatsHelper = NetworkStatsHelper(this)
        database = AppDatabase.getDatabase(this)

        registerScreenReceiver()
        startForeground(NOTIFICATION_ID, buildNotification(0L, 0L, 0L, 0L, "Connecting..."))
        startTracking()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefManager.isServiceEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun registerScreenReceiver() {
        screenReceiver = ScreenReceiver { isScreenOn ->
            pollingIntervalMs = if (isScreenOn) 1000L else 5000L
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun startTracking() {
        lastTotalRx = TrafficStats.getTotalRxBytes()
        lastTotalTx = TrafficStats.getTotalTxBytes()
        lastCheckTime = System.currentTimeMillis()

        trackerJob = serviceScope.launch {
            while (isActive) {
                delay(pollingIntervalMs)
                updateSpeed()
            }
        }
    }

    private suspend fun updateSpeed() {
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()

        val timeDiff = (currentTime - lastCheckTime).coerceAtLeast(1L)
        val rxDiff = (currentRx - lastTotalRx).coerceAtLeast(0L)
        val txDiff = (currentTx - lastTotalTx).coerceAtLeast(0L)

        // Calculate speed in bytes per second
        val rxSpeed = (rxDiff * 1000L) / timeDiff
        val txSpeed = (txDiff * 1000L) / timeDiff

        lastTotalRx = currentRx
        lastTotalTx = currentTx
        lastCheckTime = currentTime

        // Query today's data usage and network info
        val (mobileBytes, wifiBytes) = networkStatsHelper.getTodayUsage()
        val networkInfo = networkStatsHelper.getNetworkInfo()

        // Sync with local Room Database
        val today = SpeedUtils.getTodayDateString()
        val totalBytes = mobileBytes + wifiBytes
        database.dailyUsageDao().insertOrUpdate(
            DailyUsageEntity(
                date = today,
                mobileBytes = mobileBytes,
                wifiBytes = wifiBytes,
                totalBytes = totalBytes,
                updatedAt = currentTime
            )
        )

        // Check daily mobile quota alerts
        checkDataLimitAlerts(mobileBytes, today)

        // Update Notification
        val notification = buildNotification(
            rxSpeed = rxSpeed,
            txSpeed = txSpeed,
            mobileBytes = mobileBytes,
            wifiBytes = wifiBytes,
            networkName = networkInfo.name
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Update StateFlow for UI subscribers
        _liveSpeedFlow.value = SpeedData(
            downloadBytesPerSec = rxSpeed,
            uploadBytesPerSec = txSpeed,
            todayMobileBytes = mobileBytes,
            todayWifiBytes = wifiBytes,
            networkName = networkInfo.name,
            networkType = networkInfo.type
        )
    }

    private fun checkDataLimitAlerts(mobileBytes: Long, todayDate: String) {
        val limitGb = prefManager.dailyLimitGb
        if (limitGb <= 0f) return

        val limitBytes = (limitGb * 1024L * 1024L * 1024L).toLong()
        val usedRatio = mobileBytes.toDouble() / limitBytes.toDouble()

        if (usedRatio >= 1.0 && prefManager.isAlert100Enabled && !prefManager.hasAlertedToday(100, todayDate)) {
            triggerQuotaAlert(100, mobileBytes, limitBytes)
            prefManager.setAlertedToday(100, todayDate)
        } else if (usedRatio >= 0.9 && prefManager.isAlert90Enabled && !prefManager.hasAlertedToday(90, todayDate)) {
            triggerQuotaAlert(90, mobileBytes, limitBytes)
            prefManager.setAlertedToday(90, todayDate)
        } else if (usedRatio >= 0.8 && prefManager.isAlert80Enabled && !prefManager.hasAlertedToday(80, todayDate)) {
            triggerQuotaAlert(80, mobileBytes, limitBytes)
            prefManager.setAlertedToday(80, todayDate)
        }
    }

    private fun triggerQuotaAlert(percentage: Int, usedBytes: Long, limitBytes: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alert = NotificationCompat.Builder(this, NetSpeedApplication.CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_stat_speed)
            .setContentTitle(getString(R.string.alert_data_warning_title))
            .setContentText(
                getString(
                    R.string.alert_data_warning_body,
                    SpeedUtils.formatBytes(usedBytes),
                    SpeedUtils.formatBytes(limitBytes)
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID + percentage, alert)
    }

    private fun buildNotification(
        rxSpeed: Long,
        txSpeed: Long,
        mobileBytes: Long,
        wifiBytes: Long,
        networkName: String
    ): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val useBits = prefManager.useBitsUnit
        val (dlVal, dlUnit) = SpeedUtils.formatSpeed(rxSpeed, useBits)
        val (ulVal, ulUnit) = SpeedUtils.formatSpeed(txSpeed, useBits)

        val dlFormatted = "$dlVal $dlUnit"
        val ulFormatted = "$ulVal $ulUnit"

        val remoteViews = RemoteViews(packageName, R.layout.layout_speed_notification).apply {
            setTextViewText(R.id.notif_download_speed, dlFormatted)
            setTextViewText(R.id.notif_upload_speed, ulFormatted)
            setTextViewText(R.id.notif_mobile_usage, "Mobile: ${SpeedUtils.formatBytes(mobileBytes)}")
            setTextViewText(R.id.notif_wifi_usage, "Wi-Fi: ${SpeedUtils.formatBytes(wifiBytes)}")
            setTextViewText(R.id.notif_network_name, "Connected: $networkName")
        }

        val titleText = "⬇ $dlFormatted   ⬆ $ulFormatted"
        val contentText = "Mobile: ${SpeedUtils.formatBytes(mobileBytes)} | Wi-Fi: ${SpeedUtils.formatBytes(wifiBytes)}"

        val totalSpeed = rxSpeed + txSpeed
        val dynamicSpeedIcon = SpeedIconGenerator.createSpeedIcon(totalSpeed, useBits)

        return NotificationCompat.Builder(this, NetSpeedApplication.CHANNEL_SPEED_ID)
            .setSmallIcon(dynamicSpeedIcon)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setCustomBigContentView(remoteViews)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(
                if (prefManager.isHideOnLockscreen) NotificationCompat.VISIBILITY_SECRET
                else NotificationCompat.VISIBILITY_PUBLIC
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        trackerJob?.cancel()
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // Already unregistered
            }
        }
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

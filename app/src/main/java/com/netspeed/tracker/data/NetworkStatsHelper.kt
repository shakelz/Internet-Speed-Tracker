package com.netspeed.tracker.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Process
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.netspeed.tracker.data.model.AppUsageItem
import com.netspeed.tracker.utils.SpeedUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkStatsHelper(private val context: Context) {

    private val networkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Query total bytes consumed (Rx + Tx) for a network type between startTime and endTime
     */
    fun getDeviceBytes(networkType: Int, startTime: Long, endTime: Long): Long {
        if (!hasUsageStatsPermission() || networkStatsManager == null) return 0L
        return try {
            val bucket = networkStatsManager.querySummaryForDevice(
                networkType,
                null,
                startTime,
                endTime
            )
            bucket.rxBytes + bucket.txBytes
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get Today's Mobile Data and Wi-Fi Data in bytes
     */
    fun getTodayUsage(): Pair<Long, Long> {
        val startOfToday = SpeedUtils.getStartOfTodayMillis()
        val now = System.currentTimeMillis()

        val mobileBytes = getDeviceBytes(ConnectivityManager.TYPE_MOBILE, startOfToday, now)
        val wifiBytes = getDeviceBytes(ConnectivityManager.TYPE_WIFI, startOfToday, now)

        return Pair(mobileBytes, wifiBytes)
    }

    /**
     * Query data consumption for each installed app
     */
    suspend fun getAppWiseUsage(startTime: Long, endTime: Long): List<AppUsageItem> =
        withContext(Dispatchers.IO) {
            if (!hasUsageStatsPermission() || networkStatsManager == null) {
                return@withContext emptyList<AppUsageItem>()
            }

            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val items = mutableListOf<AppUsageItem>()

            // Aggregate by UID first to avoid duplicate calls
            val uidMap = mutableMapOf<Int, MutableList<ApplicationInfo>>()
            for (app in installedApps) {
                uidMap.getOrPut(app.uid) { mutableListOf() }.add(app)
            }

            for ((uid, apps) in uidMap) {
                val primaryApp = apps.first()

                val mobileBytes = getUidBytes(ConnectivityManager.TYPE_MOBILE, uid, startTime, endTime)
                val wifiBytes = getUidBytes(ConnectivityManager.TYPE_WIFI, uid, startTime, endTime)
                val totalBytes = mobileBytes + wifiBytes

                if (totalBytes > 0L) {
                    val appName = try {
                        pm.getApplicationLabel(primaryApp).toString()
                    } catch (e: Exception) {
                        primaryApp.packageName
                    }

                    val icon = try {
                        pm.getApplicationIcon(primaryApp)
                    } catch (e: Exception) {
                        null
                    }

                    items.add(
                        AppUsageItem(
                            packageName = primaryApp.packageName,
                            appName = appName,
                            icon = icon,
                            mobileBytes = mobileBytes,
                            wifiBytes = wifiBytes,
                            totalBytes = totalBytes
                        )
                    )
                }
            }

            items.sortByDescending { it.totalBytes }

            val maxUsage = items.firstOrNull()?.totalBytes ?: 1L
            return@withContext items.map { item ->
                val pct = ((item.totalBytes.toDouble() / maxUsage) * 100).toInt().coerceIn(1, 100)
                item.copy(percentageOfMax = pct)
            }
        }

    private fun getUidBytes(networkType: Int, uid: Int, startTime: Long, endTime: Long): Long {
        if (networkStatsManager == null) return 0L
        return try {
            val stats = networkStatsManager.queryDetailsForUid(
                networkType,
                null,
                startTime,
                endTime,
                uid
            )
            var total = 0L
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                total += bucket.rxBytes + bucket.txBytes
            }
            stats.close()
            total
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get details of currently active network connection
     */
    fun getNetworkInfo(): NetworkDetails {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkDetails("Offline", "None", 0, "N/A", "N/A")

        val activeNetwork = cm.activeNetwork ?: return NetworkDetails("Offline", "None", 0, "N/A", "N/A")
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkDetails("Offline", "None", 0, "N/A", "N/A")

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo: WifiInfo? = wifiManager?.connectionInfo
            val ssid = wifiInfo?.ssid?.replace("\"", "") ?: "Wi-Fi Connected"
            val linkSpeed = wifiInfo?.linkSpeed ?: 0
            val rssi = wifiInfo?.rssi ?: -100
            val freq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && wifiInfo != null) {
                if (wifiInfo.frequency > 4900) "5 GHz" else "2.4 GHz"
            } else "Wi-Fi"

            return NetworkDetails(
                type = "Wi-Fi",
                name = if (ssid == "<unknown ssid>") "Wi-Fi Connected" else ssid,
                signalLevel = rssi,
                linkSpeed = "$linkSpeed Mbps",
                extra = freq
            )
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val carrier = tm?.networkOperatorName?.ifBlank { "Mobile Cellular" } ?: "Mobile Cellular"

            return NetworkDetails(
                type = "Cellular",
                name = carrier,
                signalLevel = -75,
                linkSpeed = "Mobile Data",
                extra = "4G / 5G LTE"
            )
        }

        return NetworkDetails("Connected", "Other", 0, "N/A", "N/A")
    }

    data class NetworkDetails(
        val type: String,
        val name: String,
        val signalLevel: Int,
        val linkSpeed: String,
        val extra: String
    )
}

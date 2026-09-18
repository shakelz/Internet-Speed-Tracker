package com.netspeed.tracker.data.pref

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("netspeed_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVICE_ENABLED = "key_service_enabled"
        private const val KEY_DAILY_LIMIT_GB = "key_daily_limit_gb"
        private const val KEY_ALERT_80 = "key_alert_80"
        private const val KEY_ALERT_90 = "key_alert_90"
        private const val KEY_ALERT_100 = "key_alert_100"
        private const val KEY_FLOATING_BUBBLE = "key_floating_bubble"
        private const val KEY_USE_BITS = "key_use_bits"
        private const val KEY_AUTO_START_BOOT = "key_auto_start_boot"
        private const val KEY_HIDE_LOCKSCREEN = "key_hide_lockscreen"
        private const val KEY_AMOLED_THEME = "key_amoled_theme"

        // Last alerted dates to prevent repetitive alerts
        private const val KEY_LAST_ALERT_80_DATE = "key_last_alert_80_date"
        private const val KEY_LAST_ALERT_90_DATE = "key_last_alert_90_date"
        private const val KEY_LAST_ALERT_100_DATE = "key_last_alert_100_date"
    }

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    var dailyLimitGb: Float
        get() = prefs.getFloat(KEY_DAILY_LIMIT_GB, 1.5f)
        set(value) = prefs.edit().putFloat(KEY_DAILY_LIMIT_GB, value).apply()

    var isAlert80Enabled: Boolean
        get() = prefs.getBoolean(KEY_ALERT_80, true)
        set(value) = prefs.edit().putBoolean(KEY_ALERT_80, value).apply()

    var isAlert90Enabled: Boolean
        get() = prefs.getBoolean(KEY_ALERT_90, true)
        set(value) = prefs.edit().putBoolean(KEY_ALERT_90, value).apply()

    var isAlert100Enabled: Boolean
        get() = prefs.getBoolean(KEY_ALERT_100, true)
        set(value) = prefs.edit().putBoolean(KEY_ALERT_100, value).apply()

    var isFloatingBubbleEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_BUBBLE, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_BUBBLE, value).apply()

    var useBitsUnit: Boolean
        get() = prefs.getBoolean(KEY_USE_BITS, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_BITS, value).apply()

    var isAutoStartOnBootEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_BOOT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START_BOOT, value).apply()

    var isHideOnLockscreen: Boolean
        get() = prefs.getBoolean(KEY_HIDE_LOCKSCREEN, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_LOCKSCREEN, value).apply()

    var isAmoledTheme: Boolean
        get() = prefs.getBoolean(KEY_AMOLED_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_AMOLED_THEME, value).apply()

    fun hasAlertedToday(threshold: Int, date: String): Boolean {
        val key = when (threshold) {
            80 -> KEY_LAST_ALERT_80_DATE
            90 -> KEY_LAST_ALERT_90_DATE
            else -> KEY_LAST_ALERT_100_DATE
        }
        return prefs.getString(key, "") == date
    }

    fun setAlertedToday(threshold: Int, date: String) {
        val key = when (threshold) {
            80 -> KEY_LAST_ALERT_80_DATE
            90 -> KEY_LAST_ALERT_90_DATE
            else -> KEY_LAST_ALERT_100_DATE
        }
        prefs.edit().putString(key, date).apply()
    }
}

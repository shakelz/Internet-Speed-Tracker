package com.netspeed.tracker.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SpeedUtils {

    fun formatSpeed(bytesPerSec: Long, isBits: Boolean = false): Pair<String, String> {
        val safeBytes = if (bytesPerSec < 0) 0L else bytesPerSec

        if (isBits) {
            val bitsPerSec = safeBytes * 8.0
            return when {
                bitsPerSec >= 1_000_000_000 -> String.format(Locale.US, "%.1f", bitsPerSec / 1_000_000_000) to "Gbps"
                bitsPerSec >= 1_000_000 -> String.format(Locale.US, "%.1f", bitsPerSec / 1_000_000) to "Mbps"
                bitsPerSec >= 1_000 -> String.format(Locale.US, "%.1f", bitsPerSec / 1_000) to "Kbps"
                else -> String.format(Locale.US, "%.0f", bitsPerSec) to "bps"
            }
        } else {
            val bytes = safeBytes.toDouble()
            return when {
                bytes >= 1_073_741_824 -> String.format(Locale.US, "%.1f", bytes / 1_073_741_824) to "GB/s"
                bytes >= 1_048_576 -> String.format(Locale.US, "%.1f", bytes / 1_048_576) to "MB/s"
                bytes >= 1_024 -> String.format(Locale.US, "%.1f", bytes / 1_024) to "KB/s"
                else -> String.format(Locale.US, "%.0f", bytes) to "B/s"
            }
        }
    }

    fun formatBytes(bytes: Long): String {
        val safeBytes = if (bytes < 0) 0L else bytes
        val b = safeBytes.toDouble()
        return when {
            b >= 1_073_741_824 -> String.format(Locale.US, "%.2f GB", b / 1_073_741_824)
            b >= 1_048_576 -> String.format(Locale.US, "%.1f MB", b / 1_048_576)
            b >= 1_024 -> String.format(Locale.US, "%.1f KB", b / 1_024)
            else -> "$safeBytes B"
        }
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun getStartOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getStartOfYesterdayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getStartOfMonthMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

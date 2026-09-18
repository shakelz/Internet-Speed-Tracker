package com.netspeed.tracker.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat
import java.util.Locale

object SpeedIconGenerator {

    private const val ICON_SIZE = 96

    fun createSpeedIcon(bytesPerSec: Long, isBits: Boolean = false): IconCompat {
        val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val safeBytes = if (bytesPerSec < 0) 0L else bytesPerSec

        val (valueStr, unitStr) = if (isBits) {
            val bits = safeBytes * 8.0
            when {
                bits >= 1_000_000_000 -> {
                    val v = String.format(Locale.US, "%.1f", bits / 1_000_000_000.0)
                    (if (v.endsWith(".0")) v.substringBefore(".0") else v) to "Gb/s"
                }
                bits >= 100_000_000 -> {
                    String.format(Locale.US, "%.0f", bits / 1_000_000.0) to "Mb/s"
                }
                bits >= 1_000_000 -> {
                    val v = String.format(Locale.US, "%.1f", bits / 1_000_000.0)
                    (if (v.endsWith(".0")) v.substringBefore(".0") else v) to "Mb/s"
                }
                bits >= 1_000 -> {
                    String.format(Locale.US, "%.0f", bits / 1_000.0) to "Kb/s"
                }
                else -> "0" to "b/s"
            }
        } else {
            val bytes = safeBytes.toDouble()
            when {
                bytes >= 1_073_741_824 -> {
                    val v = String.format(Locale.US, "%.1f", bytes / 1_073_741_824.0)
                    (if (v.endsWith(".0")) v.substringBefore(".0") else v) to "GB/s"
                }
                bytes >= 104_857_600 -> {
                    String.format(Locale.US, "%.0f", bytes / 1_048_576.0) to "MB/s"
                }
                bytes >= 1_048_576 -> {
                    val v = String.format(Locale.US, "%.1f", bytes / 1_048_576.0)
                    (if (v.endsWith(".0")) v.substringBefore(".0") else v) to "MB/s"
                }
                bytes >= 1_024 -> {
                    String.format(Locale.US, "%.0f", bytes / 1_024.0) to "KB/s"
                }
                else -> "0" to "KB/s"
            }
        }

        // Draw Value (Top)
        val paintValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = when {
                valueStr.length <= 2 -> 50f
                valueStr.length == 3 -> 42f
                else -> 34f
            }
        }

        // Draw Unit (Bottom)
        val paintUnit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = when {
                unitStr.length <= 4 -> 25f
                else -> 20f
            }
        }

        canvas.drawText(valueStr, ICON_SIZE / 2f, 48f, paintValue)
        canvas.drawText(unitStr, ICON_SIZE / 2f, 82f, paintUnit)

        return IconCompat.createWithBitmap(bitmap)
    }
}

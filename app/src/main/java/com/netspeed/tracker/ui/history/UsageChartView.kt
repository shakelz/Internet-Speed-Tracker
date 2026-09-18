package com.netspeed.tracker.ui.history

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.netspeed.tracker.data.db.DailyUsageEntity
import java.text.SimpleDateFormat
import java.util.Locale

class UsageChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mobilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100") // Mobile Data Orange
        style = Paint.Style.FILL
    }

    private val wifiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676") // Wi-Fi Green
        style = Paint.Style.FILL
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E2430")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90A4AE")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private var usageList: List<DailyUsageEntity> = emptyList()

    fun setUsageData(data: List<DailyUsageEntity>) {
        // Show up to 7 days, reverse so oldest is left, today is right
        this.usageList = data.take(7).reversed()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (usageList.isEmpty()) return

        val count = usageList.size
        val availableWidth = width.toFloat()
        val availableHeight = height.toFloat() - 50f // Leave space for date labels
        val barWidth = (availableWidth / count) * 0.45f
        val step = availableWidth / count

        val maxTotal = usageList.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1024L * 1024L * 500L) ?: 1L

        val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outFormat = SimpleDateFormat("EEE", Locale.US)

        for (i in 0 until count) {
            val item = usageList[i]
            val centerX = (i * step) + (step / 2f)
            val left = centerX - (barWidth / 2f)
            val right = centerX + (barWidth / 2f)

            // Draw background slot
            val trackRect = RectF(left, 10f, right, availableHeight)
            canvas.drawRoundRect(trackRect, 10f, 10f, trackPaint)

            val totalRatio = (item.totalBytes.toFloat() / maxTotal).coerceIn(0f, 1f)
            val barTotalHeight = availableHeight * totalRatio

            val mobileRatio = if (item.totalBytes > 0) item.mobileBytes.toFloat() / item.totalBytes else 0f
            val mobileBarHeight = barTotalHeight * mobileRatio
            val wifiBarHeight = barTotalHeight - mobileBarHeight

            val bottom = availableHeight
            val mobileTop = bottom - mobileBarHeight
            val wifiTop = mobileTop - wifiBarHeight

            // Draw Mobile part (Bottom)
            if (mobileBarHeight > 0) {
                val mobileRect = RectF(left, mobileTop, right, bottom)
                canvas.drawRoundRect(mobileRect, 8f, 8f, mobilePaint)
            }

            // Draw Wi-Fi part (Top)
            if (wifiBarHeight > 0) {
                val wifiRect = RectF(left, wifiTop, right, mobileTop)
                canvas.drawRoundRect(wifiRect, 8f, 8f, wifiPaint)
            }

            // Draw Day label
            val dayLabel = try {
                val date = inFormat.parse(item.date)
                if (date != null) outFormat.format(date) else item.date.takeLast(2)
            } catch (e: Exception) {
                item.date.takeLast(2)
            }

            canvas.drawText(dayLabel, centerX, height.toFloat() - 10f, textPaint)
        }
    }
}

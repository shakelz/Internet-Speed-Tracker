package com.netspeed.tracker.ui.speedtest

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin

class SpeedometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#263238")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 6f
        color = Color.parseColor("#00E5FF")
    }

    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFFFFF")
    }

    private val arcRect = RectF()
    private val startAngle = 140f
    private val sweepAngleTotal = 260f

    private var currentSpeed: Float = 0f
    private var maxSpeed: Float = 100f
    private var animatedFraction: Float = 0f

    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 40f
        val diameter = minOf(w - padding * 2, (h - padding * 2) * 1.6f)
        val left = (w - diameter) / 2f
        val top = padding
        arcRect.set(left, top, left + diameter, top + diameter)

        // Setup vibrant neon gradient for progress arc
        val colors = intArrayOf(
            Color.parseColor("#00E5FF"),
            Color.parseColor("#00E676"),
            Color.parseColor("#FF9100"),
            Color.parseColor("#00E5FF")
        )
        progressPaint.shader = SweepGradient(arcRect.centerX(), arcRect.centerY(), colors, null)
    }

    fun setSpeed(speed: Float, max: Float = 100f) {
        this.currentSpeed = speed.coerceAtLeast(0f)
        this.maxSpeed = max.coerceAtLeast(1f)
        val targetFraction = (currentSpeed / maxSpeed).coerceIn(0f, 1f)
        val view = this

        animator?.cancel()
        animator = ValueAnimator.ofFloat(animatedFraction, targetFraction).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                view.animatedFraction = anim.animatedValue as Float
                view.invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw Background Track Arc
        canvas.drawArc(arcRect, startAngle, sweepAngleTotal, false, trackPaint)

        // 2. Draw Active Progress Arc
        val currentSweep = sweepAngleTotal * animatedFraction
        if (currentSweep > 0) {
            canvas.drawArc(arcRect, startAngle, currentSweep, false, progressPaint)
        }

        // 3. Draw Needle
        val needleAngleRad = Math.toRadians((startAngle + currentSweep).toDouble())
        val radius = (arcRect.width() / 2f) - 30f
        val cx = arcRect.centerX()
        val cy = arcRect.centerY()

        val endX = cx + radius * cos(needleAngleRad).toFloat()
        val endY = cy + radius * sin(needleAngleRad).toFloat()

        canvas.drawLine(cx, cy, endX, endY, needlePaint)
        canvas.drawCircle(cx, cy, 12f, hubPaint)
    }
}

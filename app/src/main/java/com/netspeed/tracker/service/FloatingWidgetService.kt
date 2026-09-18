package com.netspeed.tracker.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.netspeed.tracker.R
import com.netspeed.tracker.data.pref.PreferenceManager
import com.netspeed.tracker.ui.MainActivity
import com.netspeed.tracker.utils.SpeedUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingWidgetService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var flowJob: Job? = null
    private lateinit var prefManager: PreferenceManager

    companion object {
        var isRunning: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        prefManager = PreferenceManager(this)

        if (!android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)
        windowManager?.addView(floatingView, params)
        isRunning = true

        setupTouchListener(params)
        observeLiveSpeed()
    }

    private fun setupTouchListener(params: WindowManager.LayoutParams) {
        val root = floatingView?.findViewById<View>(R.id.floating_root) ?: return
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(intent)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun observeLiveSpeed() {
        val tvDl = floatingView?.findViewById<TextView>(R.id.tv_floating_dl)
        val tvUl = floatingView?.findViewById<TextView>(R.id.tv_floating_ul)

        flowJob = serviceScope.launch {
            SpeedNotificationService.liveSpeedFlow.collectLatest { speedData ->
                val useBits = prefManager.useBitsUnit
                val (dlVal, dlUnit) = SpeedUtils.formatSpeed(speedData.downloadBytesPerSec, useBits)
                val (ulVal, ulUnit) = SpeedUtils.formatSpeed(speedData.uploadBytesPerSec, useBits)

                tvDl?.text = "$dlVal $dlUnit"
                tvUl?.text = "$ulVal $ulUnit"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        flowJob?.cancel()
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
        }
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

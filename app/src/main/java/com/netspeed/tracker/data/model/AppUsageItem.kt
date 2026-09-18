package com.netspeed.tracker.data.model

import android.graphics.drawable.Drawable

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val mobileBytes: Long,
    val wifiBytes: Long,
    val totalBytes: Long,
    val percentageOfMax: Int = 0
)

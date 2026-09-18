package com.netspeed.tracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey
    val date: String,            // Format: YYYY-MM-DD
    val mobileBytes: Long = 0L,  // SIM Cellular data
    val wifiBytes: Long = 0L,    // Wi-Fi data
    val totalBytes: Long = 0L,   // mobileBytes + wifiBytes
    val updatedAt: Long = System.currentTimeMillis()
)

package com.netspeed.tracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usage: DailyUsageEntity)

    @Query("SELECT * FROM daily_usage WHERE date = :date LIMIT 1")
    suspend fun getUsageByDate(date: String): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT 7")
    fun getLast7Days(): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage ORDER BY date DESC LIMIT 30")
    fun getLast30Days(): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage ORDER BY date DESC")
    suspend fun getAllHistory(): List<DailyUsageEntity>

    @Query("SELECT SUM(totalBytes) FROM daily_usage")
    fun getTotalAllTime(): Flow<Long?>
}

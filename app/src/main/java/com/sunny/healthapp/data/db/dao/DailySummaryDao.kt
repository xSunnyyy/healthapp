package com.sunny.healthapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunny.healthapp.data.db.entities.DailySummaryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailySummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailySummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DailySummaryEntity>)

    @Query("SELECT * FROM daily_summary WHERE date = :date LIMIT 1")
    suspend fun get(date: LocalDate): DailySummaryEntity?

    @Query("SELECT * FROM daily_summary WHERE date = :date LIMIT 1")
    fun observe(date: LocalDate): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summary WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun range(start: LocalDate, end: LocalDate): List<DailySummaryEntity>

    @Query("SELECT * FROM daily_summary WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DailySummaryEntity>>

    @Query("SELECT MAX(date) FROM daily_summary")
    suspend fun latestStored(): LocalDate?
}

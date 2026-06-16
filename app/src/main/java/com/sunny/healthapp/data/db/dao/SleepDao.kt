package com.sunny.healthapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sunny.healthapp.data.db.entities.SleepSessionEntity
import com.sunny.healthapp.data.db.entities.SleepStageEntity
import java.time.Instant

@Dao
interface SleepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SleepSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStages(stages: List<SleepStageEntity>)

    @Query("DELETE FROM sleep_stage WHERE sessionId = :sessionId")
    suspend fun deleteStagesFor(sessionId: String)

    @Transaction
    suspend fun saveFull(session: SleepSessionEntity, stages: List<SleepStageEntity>) {
        upsertSession(session)
        deleteStagesFor(session.id)
        upsertStages(stages)
    }

    @Query("SELECT * FROM sleep_session WHERE `end` <= :before ORDER BY `end` DESC LIMIT 1")
    suspend fun latestBefore(before: Instant): SleepSessionEntity?

    @Query("SELECT * FROM sleep_session ORDER BY `end` DESC LIMIT 1")
    suspend fun latest(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_session WHERE `start` >= :start AND `end` <= :end ORDER BY `start` ASC")
    suspend fun range(start: Instant, end: Instant): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_stage WHERE sessionId = :sessionId ORDER BY `start` ASC")
    suspend fun stagesFor(sessionId: String): List<SleepStageEntity>

    @Query("DELETE FROM sleep_session")
    suspend fun clearAll()
}

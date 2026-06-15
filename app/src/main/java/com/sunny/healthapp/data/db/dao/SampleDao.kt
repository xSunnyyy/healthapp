package com.sunny.healthapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunny.healthapp.data.db.entities.HrSampleEntity
import com.sunny.healthapp.data.db.entities.HrvSampleEntity
import com.sunny.healthapp.data.db.entities.Spo2SampleEntity
import com.sunny.healthapp.data.db.entities.SyncStateEntity
import java.time.Instant

@Dao
interface HrSampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(samples: List<HrSampleEntity>)

    @Query("SELECT * FROM hr_sample WHERE time BETWEEN :from AND :to ORDER BY time ASC")
    suspend fun range(from: Instant, to: Instant): List<HrSampleEntity>

    @Query("SELECT MAX(time) FROM hr_sample")
    suspend fun latestTime(): Instant?
}

@Dao
interface HrvSampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(samples: List<HrvSampleEntity>)

    @Query("SELECT * FROM hrv_sample WHERE time BETWEEN :from AND :to ORDER BY time ASC")
    suspend fun range(from: Instant, to: Instant): List<HrvSampleEntity>
}

@Dao
interface Spo2SampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(samples: List<Spo2SampleEntity>)

    @Query("SELECT * FROM spo2_sample WHERE time BETWEEN :from AND :to ORDER BY time ASC")
    suspend fun range(from: Instant, to: Instant): List<Spo2SampleEntity>
}

@Dao
interface SyncStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateEntity)

    @Query("SELECT * FROM sync_state WHERE recordType = :recordType LIMIT 1")
    suspend fun get(recordType: String): SyncStateEntity?
}

package com.sunny.healthapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sunny.healthapp.data.db.dao.DailySummaryDao
import com.sunny.healthapp.data.db.dao.HrSampleDao
import com.sunny.healthapp.data.db.dao.HrvSampleDao
import com.sunny.healthapp.data.db.dao.SleepDao
import com.sunny.healthapp.data.db.dao.Spo2SampleDao
import com.sunny.healthapp.data.db.dao.SyncStateDao
import com.sunny.healthapp.data.db.entities.DailySummaryEntity
import com.sunny.healthapp.data.db.entities.HrSampleEntity
import com.sunny.healthapp.data.db.entities.HrvSampleEntity
import com.sunny.healthapp.data.db.entities.SleepSessionEntity
import com.sunny.healthapp.data.db.entities.SleepStageEntity
import com.sunny.healthapp.data.db.entities.Spo2SampleEntity
import com.sunny.healthapp.data.db.entities.SyncStateEntity

@Database(
    entities = [
        DailySummaryEntity::class,
        SleepSessionEntity::class,
        SleepStageEntity::class,
        HrSampleEntity::class,
        HrvSampleEntity::class,
        Spo2SampleEntity::class,
        SyncStateEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun sleepDao(): SleepDao
    abstract fun hrSampleDao(): HrSampleDao
    abstract fun hrvSampleDao(): HrvSampleDao
    abstract fun spo2SampleDao(): Spo2SampleDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        @Volatile private var INSTANCE: HealthDatabase? = null
        fun get(context: Context): HealthDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                HealthDatabase::class.java,
                "vitals.db",
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}

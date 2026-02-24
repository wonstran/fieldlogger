package com.fieldlogger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fieldlogger.data.local.dao.EventButtonDao
import com.fieldlogger.data.local.dao.EventDao
import com.fieldlogger.data.local.entity.EventButtonEntity
import com.fieldlogger.data.local.entity.EventEntity

@Database(
    entities = [EventEntity::class, EventButtonEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FieldLoggerDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun eventButtonDao(): EventButtonDao
}

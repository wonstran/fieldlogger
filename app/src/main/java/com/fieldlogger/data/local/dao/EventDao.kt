package com.fieldlogger.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fieldlogger.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity): Long

    @Query("SELECT * FROM events ORDER BY id DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    suspend fun getAllEventsList(): List<EventEntity>

    @Query("SELECT MAX(eventIndex) FROM events")
    suspend fun getMaxIndex(): Int?

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteById(eventId: Long)

    @Query("SELECT COUNT(*) FROM events WHERE eventCode = :eventCode")
    fun getEventCount(eventCode: Int): Flow<Int>

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM events")
    fun getTotalCount(): Flow<Int>
}

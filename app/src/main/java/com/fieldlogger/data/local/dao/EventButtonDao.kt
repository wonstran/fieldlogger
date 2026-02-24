package com.fieldlogger.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fieldlogger.data.local.entity.EventButtonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventButtonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(button: EventButtonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(buttons: List<EventButtonEntity>)

    @Update
    suspend fun update(button: EventButtonEntity)

    @Delete
    suspend fun delete(button: EventButtonEntity)

    @Query("SELECT * FROM event_buttons ORDER BY code ASC")
    fun getAllButtons(): Flow<List<EventButtonEntity>>

    @Query("SELECT * FROM event_buttons ORDER BY code ASC")
    suspend fun getAllButtonsList(): List<EventButtonEntity>

    @Query("DELETE FROM event_buttons")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM event_buttons")
    suspend fun getCount(): Int
}

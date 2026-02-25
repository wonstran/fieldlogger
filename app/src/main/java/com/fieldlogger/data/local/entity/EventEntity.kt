package com.fieldlogger.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventIndex: Int = 0,
    val eventCode: Int,
    val eventName: String,
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val note: String,
    val photoPaths: String = ""  // comma-separated paths
)

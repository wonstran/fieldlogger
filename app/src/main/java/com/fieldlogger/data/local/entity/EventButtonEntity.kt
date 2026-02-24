package com.fieldlogger.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_buttons")
data class EventButtonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: Int,
    val name: String,
    val color: Long
)

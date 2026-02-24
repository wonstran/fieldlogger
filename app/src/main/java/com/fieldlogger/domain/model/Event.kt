package com.fieldlogger.domain.model

data class Event(
    val id: Long = 0,
    val eventIndex: Int = 0,
    val eventCode: Int,
    val eventName: String,
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val note: String = "",
    val photoPath: String? = null
)

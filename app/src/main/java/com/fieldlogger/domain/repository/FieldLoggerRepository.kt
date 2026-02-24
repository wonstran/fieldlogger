package com.fieldlogger.domain.repository

import com.fieldlogger.domain.model.Event
import com.fieldlogger.domain.model.EventButton
import kotlinx.coroutines.flow.Flow

interface FieldLoggerRepository {
    suspend fun saveEvent(event: Event): Long
    fun getAllEvents(): Flow<List<Event>>
    suspend fun getAllEventsList(): List<Event>
    suspend fun getNextIndex(): Int
    suspend fun updateEvent(event: Event)
    suspend fun updateEventPhoto(eventId: Long, photoPath: String)
    suspend fun getLastEvent(): Event?
    suspend fun deleteEventById(eventId: Long)
    fun getEventCount(eventCode: Int): Flow<Int>
    fun getTotalEventCount(): Flow<Int>
    suspend fun deleteAllEvents()

    fun getAllButtons(): Flow<List<EventButton>>
    suspend fun getAllButtonsList(): List<EventButton>
    suspend fun saveButton(button: EventButton): Long
    suspend fun saveButtons(buttons: List<EventButton>)
    suspend fun deleteButton(button: EventButton)
    suspend fun getButtonsCount(): Int
    suspend fun deleteAllButtons()
}

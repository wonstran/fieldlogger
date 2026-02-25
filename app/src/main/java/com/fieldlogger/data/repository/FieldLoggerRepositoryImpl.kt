package com.fieldlogger.data.repository

import com.fieldlogger.data.local.dao.EventButtonDao
import com.fieldlogger.data.local.dao.EventDao
import com.fieldlogger.data.local.entity.EventButtonEntity
import com.fieldlogger.data.local.entity.EventEntity
import com.fieldlogger.domain.model.Event
import com.fieldlogger.domain.model.EventButton
import com.fieldlogger.domain.repository.FieldLoggerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FieldLoggerRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val eventButtonDao: EventButtonDao
) : FieldLoggerRepository {

    override suspend fun saveEvent(event: Event): Long {
        val nextIndex = eventDao.getMaxIndex()?.plus(1) ?: 1
        val entity = EventEntity(
            eventIndex = nextIndex,
            eventCode = event.eventCode,
            eventName = event.eventName,
            timestamp = event.timestamp,
            latitude = event.latitude,
            longitude = event.longitude,
            accuracy = event.accuracy,
            note = event.note,
            photoPaths = event.photoPaths.joinToString(",")
        )
        return eventDao.insert(entity)
    }

    override fun getAllEvents(): Flow<List<Event>> {
        return eventDao.getAllEvents().map { entities ->
            entities.map { entity ->
                Event(
                    id = entity.id,
                    eventIndex = entity.eventIndex,
                    eventCode = entity.eventCode,
                    eventName = entity.eventName,
                    timestamp = entity.timestamp,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    accuracy = entity.accuracy,
                    note = entity.note,
                    photoPaths = if (entity.photoPaths.isBlank()) emptyList() else entity.photoPaths.split(",")
                )
            }
        }
    }

    override suspend fun getAllEventsList(): List<Event> {
        return eventDao.getAllEventsList().map { entity ->
            Event(
                id = entity.id,
                eventIndex = entity.eventIndex,
                eventCode = entity.eventCode,
                eventName = entity.eventName,
                timestamp = entity.timestamp,
                latitude = entity.latitude,
                longitude = entity.longitude,
                accuracy = entity.accuracy,
                note = entity.note,
                photoPaths = if (entity.photoPaths.isBlank()) emptyList() else entity.photoPaths.split(",")
            )
        }
    }

    override suspend fun getNextIndex(): Int {
        return eventDao.getMaxIndex()?.plus(1) ?: 1
    }

    override suspend fun deleteEventById(eventId: Long) {
        eventDao.deleteById(eventId)
    }

    override suspend fun updateEvent(event: Event) {
        val allEvents = eventDao.getAllEventsList()
        eventDao.deleteAll()
        allEvents.filter { it.id != event.id }.forEach { entity ->
            eventDao.insert(entity)
        }
        eventDao.insert(
            EventEntity(
                id = event.id,
                eventIndex = event.eventIndex,
                eventCode = event.eventCode,
                eventName = event.eventName,
                timestamp = event.timestamp,
                latitude = event.latitude,
                longitude = event.longitude,
                accuracy = event.accuracy,
                note = event.note,
                photoPaths = event.photoPaths.joinToString(",")
            )
        )
    }

    override suspend fun addPhotoToEvent(eventId: Long, photoPath: String) {
        val allEvents = eventDao.getAllEventsList()
        eventDao.deleteAll()
        allEvents.forEach { entity ->
            if (entity.id == eventId) {
                val existingPaths = if (entity.photoPaths.isBlank()) emptyList() else entity.photoPaths.split(",")
                val newPaths = existingPaths + photoPath
                eventDao.insert(entity.copy(photoPaths = newPaths.joinToString(",")))
            } else {
                eventDao.insert(entity)
            }
        }
    }

    override suspend fun getLastEvent(): Event? {
        val events = eventDao.getAllEventsList()
        return events.firstOrNull()?.let { entity ->
            Event(
                id = entity.id,
                eventIndex = entity.eventIndex,
                eventCode = entity.eventCode,
                eventName = entity.eventName,
                timestamp = entity.timestamp,
                latitude = entity.latitude,
                longitude = entity.longitude,
                accuracy = entity.accuracy,
                note = entity.note,
                photoPaths = if (entity.photoPaths.isBlank()) emptyList() else entity.photoPaths.split(",")
            )
        }
    }

    override fun getEventCount(eventCode: Int): Flow<Int> {
        return eventDao.getEventCount(eventCode)
    }

    override fun getTotalEventCount(): Flow<Int> {
        return eventDao.getTotalCount()
    }

    override suspend fun deleteAllEvents() {
        eventDao.deleteAll()
    }

    override fun getAllButtons(): Flow<List<EventButton>> {
        return eventButtonDao.getAllButtons().map { entities ->
            entities.map { entity ->
                EventButton(
                    id = entity.id,
                    code = entity.code,
                    name = entity.name,
                    color = entity.color
                )
            }
        }
    }

    override suspend fun getAllButtonsList(): List<EventButton> {
        return eventButtonDao.getAllButtonsList().map { entity ->
            EventButton(
                id = entity.id,
                code = entity.code,
                name = entity.name,
                color = entity.color
            )
        }
    }

    override suspend fun saveButton(button: EventButton): Long {
        val entity = EventButtonEntity(
            id = button.id,
            code = button.code,
            name = button.name,
            color = button.color
        )
        return eventButtonDao.insert(entity)
    }

    override suspend fun saveButtons(buttons: List<EventButton>) {
        val entities = buttons.map { button ->
            EventButtonEntity(
                id = button.id,
                code = button.code,
                name = button.name,
                color = button.color
            )
        }
        eventButtonDao.insertAll(entities)
    }

    override suspend fun deleteButton(button: EventButton) {
        val entity = EventButtonEntity(
            id = button.id,
            code = button.code,
            name = button.name,
            color = button.color
        )
        eventButtonDao.delete(entity)
    }

    override suspend fun getButtonsCount(): Int {
        return eventButtonDao.getCount()
    }

    override suspend fun deleteAllButtons() {
        eventButtonDao.deleteAll()
    }
}

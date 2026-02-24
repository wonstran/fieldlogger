package com.fieldlogger.domain.usecase

import com.fieldlogger.domain.model.Event
import com.fieldlogger.domain.repository.FieldLoggerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllEventsUseCase @Inject constructor(
    private val repository: FieldLoggerRepository
) {
    operator fun invoke(): Flow<List<Event>> {
        return repository.getAllEvents()
    }

    suspend fun getList(): List<Event> {
        return repository.getAllEventsList()
    }
}

package com.fieldlogger.domain.usecase

import com.fieldlogger.domain.repository.FieldLoggerRepository
import javax.inject.Inject

class DeleteEventByIdUseCase @Inject constructor(
    private val repository: FieldLoggerRepository
) {
    suspend operator fun invoke(eventId: Long) {
        repository.deleteEventById(eventId)
    }
}

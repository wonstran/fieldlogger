package com.fieldlogger.domain.usecase

import com.fieldlogger.domain.model.Event
import com.fieldlogger.domain.repository.FieldLoggerRepository
import javax.inject.Inject

class GetLastEventUseCase @Inject constructor(
    private val repository: FieldLoggerRepository
) {
    suspend operator fun invoke(): Event? {
        return repository.getAllEventsList().firstOrNull()
    }
}

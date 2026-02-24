package com.fieldlogger.domain.usecase

import com.fieldlogger.domain.model.Event
import com.fieldlogger.domain.repository.FieldLoggerRepository
import javax.inject.Inject

class SaveEventUseCase @Inject constructor(
    private val repository: FieldLoggerRepository
) {
    suspend operator fun invoke(event: Event): Long {
        return repository.saveEvent(event)
    }
}

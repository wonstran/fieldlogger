package com.fieldlogger.domain.usecase

import com.fieldlogger.domain.repository.FieldLoggerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEventCountUseCase @Inject constructor(
    private val repository: FieldLoggerRepository
) {
    operator fun invoke(eventCode: Int): Flow<Int> {
        return repository.getEventCount(eventCode)
    }

    fun getTotalCount(): Flow<Int> {
        return repository.getTotalEventCount()
    }
}

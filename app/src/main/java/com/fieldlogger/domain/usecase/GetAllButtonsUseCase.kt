package com.fieldlogger.domain.usecase

import com.fieldlogger.domain.model.EventButton
import com.fieldlogger.domain.repository.FieldLoggerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllButtonsUseCase @Inject constructor(
    private val repository: FieldLoggerRepository
) {
    operator fun invoke(): Flow<List<EventButton>> {
        return repository.getAllButtons()
    }

    suspend fun getList(): List<EventButton> {
        return repository.getAllButtonsList()
    }
}

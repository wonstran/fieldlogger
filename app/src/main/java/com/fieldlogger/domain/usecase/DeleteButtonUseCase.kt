package com.fieldlogger.domain.usecase

import com.fieldlogger.domain.model.EventButton
import com.fieldlogger.domain.repository.FieldLoggerRepository
import javax.inject.Inject

class DeleteButtonUseCase @Inject constructor(
    private val repository: FieldLoggerRepository
) {
    suspend operator fun invoke(button: EventButton) {
        repository.deleteButton(button)
    }
}

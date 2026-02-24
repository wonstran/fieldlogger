package com.fieldlogger.domain.usecase

import com.fieldlogger.domain.model.EventButton
import com.fieldlogger.domain.repository.FieldLoggerRepository
import javax.inject.Inject

class SaveButtonsUseCase @Inject constructor(
    private val repository: FieldLoggerRepository
) {
    suspend operator fun invoke(buttons: List<EventButton>) {
        repository.saveButtons(buttons)
    }

    suspend fun getCount(): Int {
        return repository.getButtonsCount()
    }
}

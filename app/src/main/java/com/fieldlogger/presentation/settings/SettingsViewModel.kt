package com.fieldlogger.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldlogger.domain.model.EventButton
import com.fieldlogger.domain.usecase.GetAllButtonsUseCase
import com.fieldlogger.domain.usecase.SaveButtonsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val buttons: List<EventButton> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingButton: EventButton? = null,
    val buttonName: String = "",
    val selectedColor: Long = 0xFF1565C0
)

sealed class SettingsUiEvent {
    data class ShowSnackbar(val message: String) : SettingsUiEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getAllButtonsUseCase: GetAllButtonsUseCase,
    private val saveButtonsUseCase: SaveButtonsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsUiEvent>()
    val events = _events.asSharedFlow()

    private val colorOptions = listOf(
        0xFFE53935,
        0xFFFB8C00,
        0xFF43A047,
        0xFF1E88E5,
        0xFF8E24AA,
        0xFF00ACC1,
        0xFF6D4C41,
        0xFF546E7A
    )

    init {
        loadButtons()
    }

    private fun loadButtons() {
        viewModelScope.launch {
            getAllButtonsUseCase().collect { buttons ->
                _uiState.value = _uiState.value.copy(buttons = buttons)
            }
        }
    }

    fun onAddButtonClick() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingButton = null,
            buttonName = "",
            selectedColor = colorOptions[_uiState.value.buttons.size % colorOptions.size]
        )
    }

    fun onEditButtonClick(button: EventButton) {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingButton = button,
            buttonName = button.name,
            selectedColor = button.color
        )
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(buttonName = name)
    }

    fun onColorSelected(color: Long) {
        _uiState.value = _uiState.value.copy(selectedColor = color)
    }

    fun onSaveButton() {
        val state = _uiState.value
        val name = state.buttonName.trim()
        if (name.isBlank()) {
            viewModelScope.launch {
                _events.emit(SettingsUiEvent.ShowSnackbar("Please enter a name"))
            }
            return
        }

        viewModelScope.launch {
            val existingButtons = state.buttons.toMutableList()

            if (state.editingButton != null) {
                val index = existingButtons.indexOfFirst { it.code == state.editingButton.code }
                if (index >= 0) {
                    existingButtons[index] = state.editingButton.copy(name = name, color = state.selectedColor)
                }
            } else {
                val newCode = if (existingButtons.isEmpty()) 1 else (existingButtons.maxOf { it.code } + 1)
                existingButtons.add(EventButton(code = newCode, name = name, color = state.selectedColor))
            }

            saveButtonsUseCase(existingButtons)
            _uiState.value = _uiState.value.copy(showAddDialog = false)
            _events.emit(SettingsUiEvent.ShowSnackbar("Button saved"))
        }
    }

    fun onDeleteButton(button: EventButton) {
        viewModelScope.launch {
            val existingButtons = _uiState.value.buttons.filter { it.code != button.code }
            saveButtonsUseCase(existingButtons)
            _events.emit(SettingsUiEvent.ShowSnackbar("Button deleted"))
        }
    }

    fun onDismissDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun getColorOptions(): List<Long> = colorOptions
}

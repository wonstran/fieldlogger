package com.fieldlogger.presentation.main

import android.content.Intent
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldlogger.domain.model.Event
import com.fieldlogger.domain.model.EventButton
import com.fieldlogger.domain.usecase.DeleteAllEventsUseCase
import com.fieldlogger.domain.usecase.DeleteEventByIdUseCase
import com.fieldlogger.domain.usecase.GetAllButtonsUseCase
import com.fieldlogger.domain.usecase.GetAllEventsUseCase
import com.fieldlogger.domain.usecase.GetEventCountUseCase
import com.fieldlogger.domain.usecase.GetLastEventUseCase
import com.fieldlogger.domain.usecase.SaveButtonsUseCase
import com.fieldlogger.domain.usecase.SaveEventUseCase
import com.fieldlogger.domain.repository.FieldLoggerRepository
import com.fieldlogger.util.CsvExporter
import com.fieldlogger.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MainUiState(
    val buttons: List<EventButton> = emptyList(),
    val eventCounts: Map<Int, Int> = emptyMap(),
    val totalCount: Int = 0,
    val currentLocation: Location? = null,
    val currentTimestamp: String = "",
    val isLocationEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val hasEvents: Boolean = false,
    val canUndo: Boolean = false,
    val showReviewDialog: Boolean = false,
    val allEvents: List<Event> = emptyList()
)

sealed class MainUiEvent {
    data class ShowSnackbar(val message: String) : MainUiEvent()
    data class ExportSuccess(val path: String) : MainUiEvent()
    data class ShareIntent(val intent: Intent) : MainUiEvent()
    data class PhotoCaptureRequested(val eventId: Long, val eventIndex: Int) : MainUiEvent()
    object ExportError : MainUiEvent()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val saveEventUseCase: SaveEventUseCase,
    private val getAllEventsUseCase: GetAllEventsUseCase,
    private val getEventCountUseCase: GetEventCountUseCase,
    private val deleteAllEventsUseCase: DeleteAllEventsUseCase,
    private val deleteEventByIdUseCase: DeleteEventByIdUseCase,
    private val getLastEventUseCase: GetLastEventUseCase,
    private val getAllButtonsUseCase: GetAllButtonsUseCase,
    private val saveButtonsUseCase: SaveButtonsUseCase,
    private val repository: FieldLoggerRepository,
    private val locationHelper: LocationHelper,
    private val csvExporter: CsvExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainUiEvent>()
    val events = _events.asSharedFlow()

    private val locationFlow = MutableStateFlow<Location?>(null)

    init {
        viewModelScope.launch {
            val count = saveButtonsUseCase.getCount()
            if (count == 0) {
                val defaultButtons = createDefaultButtons()
                saveButtonsUseCase(defaultButtons)
            }
        }
        observeButtons()
        observeLocation()
    }

    private fun observeButtons() {
        viewModelScope.launch {
            getAllButtonsUseCase().collect { buttons ->
                _uiState.value = _uiState.value.copy(buttons = buttons)
                observeEventCounts(buttons)
            }
        }
    }

    private fun observeEventCounts(buttons: List<EventButton>) {
        viewModelScope.launch {
            if (buttons.isEmpty()) return@launch

            val countsFlows = buttons.map { button ->
                getEventCountUseCase(button.code)
            }

            combine(countsFlows) { counts ->
                buttons.mapIndexed { index, button ->
                    button.code to counts[index]
                }.toMap()
            }.collect { counts ->
                _uiState.value = _uiState.value.copy(
                    eventCounts = counts,
                    totalCount = counts.values.sum(),
                    hasEvents = counts.values.sum() > 0
                )
            }
        }
    }

    private fun observeLocation() {
        viewModelScope.launch {
            if (locationHelper.hasLocationPermission()) {
                _uiState.value = _uiState.value.copy(isLocationEnabled = true)
                locationHelper.getLocationUpdates().collect { location ->
                    locationFlow.value = location
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    _uiState.value = _uiState.value.copy(
                        currentLocation = location,
                        currentTimestamp = timestamp
                    )
                }
            }
        }
    }

    fun onButtonClick(button: EventButton) {
        val location = locationFlow.value
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(Date())

            val event = Event(
                eventCode = button.code,
                eventName = button.name,
                timestamp = timestamp,
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
                accuracy = location?.accuracy,
                note = ""
            )

            saveEventUseCase(event)
            _uiState.value = _uiState.value.copy(canUndo = true)
        }
    }

    fun onUndo() {
        viewModelScope.launch {
            val lastEvent = getLastEventUseCase()
            if (lastEvent != null) {
                deleteEventByIdUseCase(lastEvent.id)
                _uiState.value = _uiState.value.copy(canUndo = false)
                _events.emit(MainUiEvent.ShowSnackbar("Last event undone"))
            } else {
                _events.emit(MainUiEvent.ShowSnackbar("No events to undo"))
            }
        }
    }

    fun onCapturePhoto() {
        viewModelScope.launch {
            val lastEvent = repository.getLastEvent()
            if (lastEvent != null) {
                _events.emit(MainUiEvent.PhotoCaptureRequested(lastEvent.id, lastEvent.eventIndex))
            } else {
                _events.emit(MainUiEvent.ShowSnackbar("No events to attach photo"))
            }
        }
    }

    fun onPhotoCaptured(eventId: Long, photoPath: String) {
        viewModelScope.launch {
            repository.updateEventPhoto(eventId, photoPath)
            _events.emit(MainUiEvent.ShowSnackbar("Photo attached to event"))
        }
    }

    fun onReview() {
        viewModelScope.launch {
            val events = getAllEventsUseCase.getList()
            _uiState.value = _uiState.value.copy(
                showReviewDialog = true,
                allEvents = events.reversed()
            )
        }
    }

    fun onDismissReview() {
        _uiState.value = _uiState.value.copy(
            showReviewDialog = false,
            allEvents = emptyList()
        )
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            deleteEventByIdUseCase(eventId)
            val events = getAllEventsUseCase.getList()
            _uiState.value = _uiState.value.copy(
                allEvents = events.reversed()
            )
            _events.emit(MainUiEvent.ShowSnackbar("Event deleted"))
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            repository.updateEvent(event)
            val events = getAllEventsUseCase.getList()
            _uiState.value = _uiState.value.copy(
                allEvents = events.reversed()
            )
            _events.emit(MainUiEvent.ShowSnackbar("Event updated"))
        }
    }

    fun updateButtonName(button: EventButton, newName: String) {
        viewModelScope.launch {
            val currentButtons = _uiState.value.buttons.toMutableList()
            val index = currentButtons.indexOfFirst { it.code == button.code }
            if (index >= 0 && newName.isNotBlank()) {
                currentButtons[index] = button.copy(name = newName)
                saveButtonsUseCase(currentButtons)
                _events.emit(MainUiEvent.ShowSnackbar("Button updated"))
            }
        }
    }

    fun onExport(customFileName: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val events = getAllEventsUseCase.getList()

            if (events.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _events.emit(MainUiEvent.ShowSnackbar("No events to export"))
                return@launch
            }

            val result = csvExporter.exportToCsv(events, customFileName)
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.fold(
                onSuccess = { path ->
                    _events.emit(MainUiEvent.ExportSuccess(path))
                },
                onFailure = {
                    _events.emit(MainUiEvent.ExportError)
                }
            )
        }
    }

    fun onShare() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val events = getAllEventsUseCase.getList()

            if (events.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _events.emit(MainUiEvent.ShowSnackbar("No events to share"))
                return@launch
            }

            val exportResult = csvExporter.exportToCsvForShare(events)
            _uiState.value = _uiState.value.copy(isLoading = false)

            exportResult.fold(
                onSuccess = { uri ->
                    val intent = csvExporter.shareFile(uri)
                    _events.emit(MainUiEvent.ShareIntent(intent))
                },
                onFailure = {
                    _events.emit(MainUiEvent.ExportError)
                }
            )
        }
    }

    fun onClearAll() {
        viewModelScope.launch {
            deleteAllEventsUseCase()
            _events.emit(MainUiEvent.ShowSnackbar("All events cleared"))
        }
    }

    fun updateLocationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isLocationEnabled = enabled)
        if (enabled) {
            observeLocation()
        }
    }

    private fun createDefaultButtons(): List<EventButton> {
        return listOf(
            EventButton(code = 1, name = "Event 1", color = 0xFFE53935),
            EventButton(code = 2, name = "Event 2", color = 0xFFFB8C00),
            EventButton(code = 3, name = "Event 3", color = 0xFF43A047),
            EventButton(code = 4, name = "Event 4", color = 0xFF1E88E5),
            EventButton(code = 5, name = "Event 5", color = 0xFF8E24AA),
            EventButton(code = 6, name = "Event 6", color = 0xFF00ACC1)
        )
    }
}

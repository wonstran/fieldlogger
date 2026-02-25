package com.fieldlogger.presentation.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fieldlogger.BuildConfig
import com.fieldlogger.domain.model.Event
import com.fieldlogger.domain.model.EventButton
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }
    var showEditButtonDialog by remember { mutableStateOf(false) }
    var editingButton by remember { mutableStateOf<EventButton?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFileName by remember { mutableStateOf("") }
    var editButtonName by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        viewModel.updateLocationEnabled(fineLocation || coarseLocation)
    }

    var cameraPermissionGranted by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (granted) {
            viewModel.onCapturePhotoReady()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    var pendingPhotoEventId by remember { mutableStateOf<Long?>(null) }
    var pendingPhotoFileUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingPhotoEventId != null && pendingPhotoFileUri != null) {
            viewModel.onPhotoCaptured(pendingPhotoEventId!!, pendingPhotoFileUri.toString())
        }
        pendingPhotoEventId = null
        pendingPhotoFileUri = null
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is MainUiEvent.ExportSuccess -> {
                    snackbarHostState.showSnackbar("Exported to: ${event.path}")
                }
                is MainUiEvent.ExportError -> {
                    snackbarHostState.showSnackbar("Export failed")
                }
                is MainUiEvent.ShareIntent -> {
                    context.startActivity(Intent.createChooser(event.intent, "Share CSV"))
                }
                is MainUiEvent.PhotoCaptureRequested -> {
                    val timestamp = System.currentTimeMillis()
                    val fileName = "FieldLogger_${event.eventIndex}_$timestamp.jpg"
                    
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures")
                    }
                    
                    val uri = context.contentResolver.insert(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    
                    if (uri != null) {
                        pendingPhotoEventId = event.eventId
                        pendingPhotoFileUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        snackbarHostState.showSnackbar("Failed to create photo file")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Field Logger (v${BuildConfig.VERSION_NAME})") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Clear All",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { viewModel.onReview() }) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Review Events",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { viewModel.onShare() }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share CSV",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.canUndo) {
                Button(
                    onClick = { viewModel.onUndo() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Last Input", color = Color.White)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                StatusCard(
                    totalCount = uiState.totalCount,
                    isLocationEnabled = uiState.isLocationEnabled,
                    location = uiState.currentLocation,
                    timestamp = uiState.currentTimestamp
                )

                Spacer(modifier = Modifier.height(16.dp))

                EventCountsRow(
                    buttons = uiState.buttons,
                    eventCounts = uiState.eventCounts
                )

                Spacer(modifier = Modifier.height(16.dp))

                EventButtonsGrid(
                    buttons = uiState.buttons,
                    eventCounts = uiState.eventCounts,
                    onButtonClick = { viewModel.onButtonClick(it) },
                    onButtonLongClick = { button ->
                        editingButton = button
                        editButtonName = button.name
                        showEditButtonDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        shadowElevation = 8.dp,
                        onClick = {
                            if (cameraPermissionGranted) {
                                viewModel.onCapturePhotoReady()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Take Photo",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        if (uiState.showReviewDialog) {
            ReviewDialog(
                events = uiState.allEvents,
                onDismiss = { viewModel.onDismissReview() },
                onDeleteEvent = { viewModel.deleteEvent(it) },
                onEditEvent = { viewModel.updateEvent(it) }
            )
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear All Events") },
                text = { Text("Are you sure you want to delete all recorded events? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onClearAll()
                            showClearDialog = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export to CSV") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = exportFileName,
                            onValueChange = { exportFileName = it },
                            label = { Text("File Name (optional)") },
                            placeholder = { Text("Leave empty for auto-generated") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "File will be saved to Downloads folder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val fileName = exportFileName.takeIf { it.isNotBlank() }?.let {
                                if (it.endsWith(".csv")) it else "$it.csv"
                            }
                            viewModel.onExport(fileName)
                            showExportDialog = false
                            exportFileName = ""
                        }
                    ) {
                        Text("Export")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showEditButtonDialog && editingButton != null) {
            AlertDialog(
                onDismissRequest = { showEditButtonDialog = false },
                title = { Text("Edit Button Name") },
                text = {
                    OutlinedTextField(
                        value = editButtonName,
                        onValueChange = { editButtonName = it },
                        label = { Text("Button Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateButtonName(editingButton!!, editButtonName)
                            showEditButtonDialog = false
                            editingButton = null
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditButtonDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun StatusCard(
    totalCount: Int,
    isLocationEnabled: Boolean,
    location: android.location.Location?,
    timestamp: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Events",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = totalCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (timestamp.isNotEmpty()) {
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = if (isLocationEnabled) Color(0xFF43A047) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLocationEnabled && location != null) {
                            "%.5f, %.5f".format(location.latitude, location.longitude)
                        } else if (isLocationEnabled) {
                            "Acquiring..."
                        } else {
                            "No GPS"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EventCountsRow(
    buttons: List<EventButton>,
    eventCounts: Map<Int, Int>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        buttons.forEach { button ->
            val count = eventCounts[button.code] ?: 0
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(button.color).copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = button.code.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(button.color)
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(button.color)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventButtonsGrid(
    buttons: List<EventButton>,
    eventCounts: Map<Int, Int>,
    onButtonClick: (EventButton) -> Unit,
    onButtonLongClick: (EventButton) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(buttons) { button ->
            EventButtonItem(
                button = button,
                count = eventCounts[button.code] ?: 0,
                onClick = { onButtonClick(button) },
                onLongClick = { onButtonLongClick(button) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventButtonItem(
    button: EventButton,
    count: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = Color(button.color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = button.code.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = button.name,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = "$count recorded",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ReviewDialog(
    events: List<Event>,
    onDismiss: () -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onEditEvent: (Event) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }
    var showEditDialog by remember { mutableStateOf<Event?>(null) }
    var editEventName by remember { mutableStateOf("") }
    var editTimestamp by remember { mutableStateOf("") }
    var editLatitude by remember { mutableStateOf("") }
    var editLongitude by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Event History") },
        text = {
            if (events.isEmpty()) {
                Text("No events recorded yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(events) { event ->
                        SwipeableEventCard(
                            event = event,
                            onDelete = { showDeleteConfirm = event.id },
                            onLongPress = {
                                showEditDialog = event
                                editEventName = event.eventName
                                editTimestamp = event.timestamp
                                editLatitude = event.latitude.toString()
                                editLongitude = event.longitude.toString()
                                editNote = event.note
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    showDeleteConfirm?.let { eventId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Event") },
            text = { Text("Delete this event?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteEvent(eventId)
                    showDeleteConfirm = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    showEditDialog?.let { event ->
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Edit Event") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editEventName,
                        onValueChange = { editEventName = it },
                        label = { Text("Event Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editTimestamp,
                        onValueChange = { editTimestamp = it },
                        label = { Text("Timestamp") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editLatitude,
                        onValueChange = { editLatitude = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editLongitude,
                        onValueChange = { editLongitude = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val lat = editLatitude.toDoubleOrNull() ?: event.latitude
                    val lng = editLongitude.toDoubleOrNull() ?: event.longitude
                    onEditEvent(event.copy(
                        eventName = editEventName,
                        timestamp = editTimestamp,
                        latitude = lat,
                        longitude = lng,
                        note = editNote
                    ))
                    showEditDialog = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableEventCard(
    event: Event,
    onDelete: () -> Unit,
    onLongPress: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val threshold = -150f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.error)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < threshold) {
                                onDelete()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = offsetX + dragAmount
                            if (newOffset < 0) {
                                offsetX = newOffset.coerceAtMost(0f)
                            }
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "#${event.eventIndex} ${event.eventName}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = event.timestamp.takeLast(8),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "%.5f, %.5f".format(event.latitude, event.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.note.isNotBlank()) {
                    Text(
                        text = event.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!event.photoPath.isNullOrBlank()) {
                    Text(
                        text = "Photo: ${event.photoPath.substringAfterLast("/")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

package com.fieldlogger.presentation.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import coil.compose.AsyncImage
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.layout.ContentScale
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var exportFileName by remember { 
        mutableStateOf("FieldLogger_${SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())}") 
    }
    var editButtonName by remember { mutableStateOf("") }
    var showAutoExportDialog by remember { mutableStateOf(false) }
    var autoExportInterval by remember { mutableStateOf("5") }

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
                    val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
                    val safeName = event.eventName.replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                    val fileName = "FieldLogger_${event.eventIndex}_${safeName}_$timestamp.jpg"
                    
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
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Field Logger", 
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                ),
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Clear All",
                            tint = Color(0xFF007AFF)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF007AFF)
                        )
                    }
                    IconButton(onClick = { showAutoExportDialog = true }) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Auto Export Settings",
                            tint = if (uiState.autoExportEnabled) Color(0xFF34C759) else Color(0xFF007AFF)
                        )
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save CSV",
                            tint = Color(0xFF007AFF)
                        )
                    }
                    IconButton(onClick = { viewModel.onShare() }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share CSV",
                            tint = Color(0xFF007AFF)
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

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // White outer ring
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        onClick = {
                            if (cameraPermissionGranted) {
                                viewModel.onCapturePhotoReady()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        // Inner colored circle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(68.dp)
                                .background(
                                    Color(0xFF007AFF),
                                    androidx.compose.foundation.shape.CircleShape
                                )
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
                title = { 
                    Text(
                        "Clear All Events",
                        fontWeight = FontWeight(600)
                    ) 
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                text = { 
                    Text(
                        "Are you sure you want to delete all recorded events? This action cannot be undone.",
                        color = Color.Gray
                    ) 
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onClearAll()
                            showClearDialog = false
                        }
                    ) {
                        Text("Delete", color = Color(0xFFFF3B30))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel", color = Color(0xFF007AFF))
                    }
                }
            )
        }

        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { 
                    Text(
                        "Save CSV",
                        fontWeight = FontWeight(600)
                    ) 
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                text = {
                    Column {
                        OutlinedTextField(
                            value = if (exportFileName.startsWith("FieldLogger_")) exportFileName else "FieldLogger_$exportFileName",
                            onValueChange = { 
                                exportFileName = if (it.startsWith("FieldLogger_")) it else "FieldLogger_$it"
                            },
                            label = { Text("File Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Will be saved to Downloads as [name].csv",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val fileName = if (exportFileName.endsWith(".csv")) exportFileName else "$exportFileName.csv"
                            viewModel.onExport(fileName)
                            showExportDialog = false
                            exportFileName = "FieldLogger_${SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())}"
                        }
                    ) {
                        Text("Save", color = Color(0xFF007AFF))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel", color = Color(0xFF007AFF))
                    }
                }
            )
        }

        if (showAutoExportDialog) {
            var isEnabled by remember { mutableStateOf(uiState.autoExportEnabled) }
            
            AlertDialog(
                onDismissRequest = { showAutoExportDialog = false },
                title = { Text("Auto Export Settings", fontWeight = FontWeight(600)) },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                text = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Auto Export", modifier = Modifier.weight(1f))
                            androidx.compose.material3.Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = autoExportInterval,
                            onValueChange = { autoExportInterval = it.filter { c -> c.isDigit() } },
                            label = { Text("Interval (minutes)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isEnabled
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "File: FieldLogger_yyyymmdd-hhmm.csv will be saved to Downloads",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.setAutoExportEnabled(isEnabled)
                            val interval = autoExportInterval.toIntOrNull() ?: 5
                            viewModel.setAutoExportInterval(interval)
                            showAutoExportDialog = false
                        }
                    ) {
                        Text("Save", color = Color(0xFF007AFF))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAutoExportDialog = false }) {
                        Text("Cancel", color = Color(0xFF007AFF))
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
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Text(
                    text = totalCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (timestamp.isNotEmpty()) {
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = if (isLocationEnabled) Color(0xFF34C759) else Color.Gray,
                        modifier = Modifier.size(14.dp)
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
                        color = Color.Gray,
                        fontSize = 12.sp
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
            .height(110.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = Color(button.color),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = button.name,
                fontSize = 18.sp,
                fontWeight = FontWeight(600),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "$count recorded",
                    fontSize = 12.sp,
                    fontWeight = FontWeight(500),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
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
    var showPhotoDialog by remember { mutableStateOf<List<String>?>(null) }
    var currentPhotoIndex by remember { mutableStateOf(0) }
    var editEventName by remember { mutableStateOf("") }
    var editTimestamp by remember { mutableStateOf("") }
    var editLatitude by remember { mutableStateOf("") }
    var editLongitude by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Event History", 
                fontWeight = FontWeight(600)
            ) 
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        text = {
            if (events.isEmpty()) {
                Text("No events recorded yet.", color = Color.Gray)
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
                            },
                            onPhotoClick = { photoPaths ->
                                showPhotoDialog = photoPaths
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF007AFF))
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

    if (showPhotoDialog != null && showPhotoDialog!!.isNotEmpty()) {
        val currentPhoto = showPhotoDialog!![currentPhotoIndex]
        
        AlertDialog(
            onDismissRequest = { 
                showPhotoDialog = null
                currentPhotoIndex = 0
            },
            title = { 
                Text(
                    "Photo ${currentPhotoIndex + 1} of ${showPhotoDialog!!.size}", 
                    fontWeight = FontWeight(600)
                ) 
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val photoUri = try {
                        android.net.Uri.parse(currentPhoto)
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Unable to load image", color = Color.Gray)
                    }
                    
                    if (showPhotoDialog!!.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(
                                onClick = { if (currentPhotoIndex > 0) currentPhotoIndex-- },
                                enabled = currentPhotoIndex > 0
                            ) {
                                Text("Previous")
                            }
                            TextButton(
                                onClick = { if (currentPhotoIndex < showPhotoDialog!!.size - 1) currentPhotoIndex++ },
                                enabled = currentPhotoIndex < showPhotoDialog!!.size - 1
                            ) {
                                Text("Next")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showPhotoDialog = null
                    currentPhotoIndex = 0
                }) {
                    Text("Close", color = Color(0xFF007AFF))
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
    onLongPress: () -> Unit,
    onPhotoClick: (List<String>) -> Unit
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
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF007AFF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "#${event.eventIndex}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = event.eventName,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = event.timestamp.takeLast(8),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%.5f, %.5f".format(event.latitude, event.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                if (event.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF007AFF),
                        fontSize = 12.sp
                    )
                }
                if (event.photoPaths.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Photos",
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${event.photoPaths.size} photo(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF34C759),
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { 
                                onPhotoClick(event.photoPaths) 
                            }
                        )
                    }
                }
            }
        }
    }
}

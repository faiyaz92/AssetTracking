@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.locations

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assettracking.domain.model.LocationSummary
import com.example.assettracking.presentation.tabs.viewmodel.LocationListViewModel
import com.example.assettracking.presentation.tabs.model.LocationListEvent
import com.example.assettracking.util.rememberBarcodeImage
import com.example.assettracking.util.printBarcode
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun LocationsScreen(
    onBack: () -> Unit,
    onOpenLocation: (Long) -> Unit,
    viewModel: LocationListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        val message = state.message?.text
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(LocationListEvent.ClearMessage)
        }
    }

    var showRoomDialog by rememberSaveable { mutableStateOf(false) }
    var editingRoom by remember { mutableStateOf<LocationSummary?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var roomToDelete by remember { mutableStateOf<LocationSummary?>(null) }

    // Search functionality
    var searchText by rememberSaveable { mutableStateOf("") }
    var showLocationCodeSuggestions by remember { mutableStateOf(false) }
    val isLocationCodeSearch = searchText.startsWith("loc", ignoreCase = true) ||
                              searchText.startsWith("loc:", ignoreCase = true) ||
                              searchText.startsWith("loc-", ignoreCase = true)

    // Filter locations based on search
    val filteredLocations = remember(searchText, state.locations) {
        if (searchText.isBlank()) {
            state.locations
        } else if (isLocationCodeSearch) {
            // Don't filter the main list when searching by code - suggestions will be shown separately
            state.locations
        } else {
            // Normal search by name or ID
            state.locations.filter { location ->
                location.name.contains(searchText, ignoreCase = true) ||
                location.id.toString().contains(searchText) ||
                location.locationCode.contains(searchText, ignoreCase = true)
            }
        }
    }

    // Precompute descendant counts and descendant asset totals for every location
    val (descendantLocationCount, descendantAssetCount) = remember(state.locations) {
        val childrenByParent = state.locations.groupBy { it.parentId }

        fun accumulate(locationId: Long): Pair<Int, Int> {
            val children = childrenByParent[locationId] ?: emptyList()
            var totalChildren = 0
            var totalAssets = 0
            for (child in children) {
                totalChildren += 1
                totalAssets += child.assetCount
                val (childCount, childAssets) = accumulate(child.id)
                totalChildren += childCount
                totalAssets += childAssets
            }
            return totalChildren to totalAssets
        }

        val childCountMap = mutableMapOf<Long, Int>()
        val assetCountMap = mutableMapOf<Long, Int>()
        state.locations.forEach { location ->
            val (c, a) = accumulate(location.id)
            childCountMap[location.id] = c
            assetCountMap[location.id] = a
        }
        childCountMap to assetCountMap
    }

    val locationScannerLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        val contents = result.contents
        if (contents != null) {
            // Parse the location ID from the barcode (remove padding)
            val locationId = contents.toLongOrNull()
            if (locationId != null) {
                onOpenLocation(locationId)
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1E40AF), // Deep Blue
                                Color(0xFF06B6D4)  // Teal
                            )
                        )
                    )
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Location Management",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Scan Location FAB
                FloatingActionButton(
                    onClick = {
                        val scanOptions = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.CODE_128)
                            setPrompt("Scan location barcode")
                            setCameraId(0)
                            setBeepEnabled(true)
                            setOrientationLocked(true)
                        }
                        locationScannerLauncher.launch(scanOptions)
                    },
                    containerColor = Color(0xFFEA580C), // Orange color for scan
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan location")
                }

                // Add Location FAB
                FloatingActionButton(
                    onClick = {
                        editingRoom = null
                        showRoomDialog = true
                    },
                    containerColor = Color(0xFF1E40AF), // Match toolbar color
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add location")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Professional Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1E40AF), // Deep Blue - Match toolbar
                                Color(0xFF06B6D4)  // Teal - Match toolbar
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        "Manage Your Spaces",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Organize and track assets across different locations",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    showLocationCodeSuggestions = it.startsWith("loc", ignoreCase = true) ||
                                                it.startsWith("loc:", ignoreCase = true) ||
                                                it.startsWith("loc-", ignoreCase = true)
                },
                label = { Text("Search locations or type 'loc' for code search") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            // Location Code Suggestions Dropdown
            if (showLocationCodeSuggestions && searchText.isNotBlank()) {
                val searchQuery = searchText.removePrefix("loc").removePrefix(":").removePrefix("-").trim()
                val matchingLocations = state.locations.filter { location ->
                    location.locationCode.contains(searchQuery, ignoreCase = true)
                }

                if (matchingLocations.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .heightIn(max = 200.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(matchingLocations.take(10)) { location ->
                                Text(
                                    text = "${location.locationCode} - ${location.name}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchText = ""
                                            showLocationCodeSuggestions = false
                                            onOpenLocation(location.id)
                                        }
                                        .padding(12.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            when {
                state.isLoading -> LoadingState(Modifier.fillMaxSize())
                state.locations.isEmpty() -> EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    message = "No locations yet. Tap + to add your first location."
                )
                filteredLocations.isEmpty() && searchText.isNotBlank() -> EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    message = "No locations found matching '$searchText'"
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLocations, key = { it.id }) { room ->
                        RoomCard(
                            room = room,
                            descendantLocationCount = descendantLocationCount[room.id] ?: 0,
                            descendantAssetCount = descendantAssetCount[room.id] ?: 0,
                            onClick = { onOpenLocation(room.id) },
                            onEdit = {
                                editingRoom = room
                                showRoomDialog = true
                            },
                            onDelete = {
                                roomToDelete = room
                                showDeleteDialog = true
                            },
                            onPrint = {
                                printBarcode(context, room.id.toString().padStart(6, '0'), room.name)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRoomDialog) {
        val room = editingRoom
        RoomFormDialog(
            title = if (room == null) "Add Location" else "Edit Location",
            initialName = room?.name.orEmpty(),
            initialDescription = room?.description.orEmpty(),
            onDismiss = { showRoomDialog = false },
            onConfirm = { name, description ->
                if (room == null) {
                    viewModel.onEvent(LocationListEvent.CreateLocation(name, description))
                } else {
                    viewModel.onEvent(LocationListEvent.UpdateLocation(room.id, name, description))
                }
                showRoomDialog = false
            }
        )
    }

    if (showDeleteDialog && roomToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Location") },
            text = { 
                Text("Are you sure you want to delete '${roomToDelete?.name}'? This action cannot be undone and will remove all assets from this location.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        roomToDelete?.let { viewModel.onEvent(LocationListEvent.DeleteLocation(it.id)) }
                        showDeleteDialog = false
                        roomToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    roomToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RoomCard(
    room: LocationSummary,
    descendantLocationCount: Int,
    descendantAssetCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit
) {
    val (showBarcode, setShowBarcode) = remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Room Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Location ID: ${room.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Code: ${room.locationCode}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1E40AF) // Blue color for code
                    )
                }
                // Room icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E40AF),
                                    Color(0xFF3B82F6)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Room Description
            room.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Asset Count Info
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = room.assetCount.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Assets in this location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (descendantLocationCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = descendantLocationCount.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1)
                            )
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Child locations (all levels)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0EA5E9).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = descendantAssetCount.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0EA5E9)
                            )
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Assets across child locations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons
            Spacer(Modifier.height(16.dp))

            // Barcode Image
            val barcodeBitmap = rememberBarcodeImage(content = room.id.toString().padStart(6, '0'), width = 800, height = 220)
            if (showBarcode && barcodeBitmap != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = barcodeBitmap,
                        contentDescription = "Location barcode",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Action Buttons
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF1E40AF)
                    )
                ) {
                    Text("View Details")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { setShowBarcode(!showBarcode) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = if (showBarcode) "Hide barcode" else "Show barcode",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onPrint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print barcode",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit location",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete location",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun RowActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onEdit) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit location")
        }
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete location")
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier, message: String) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message)
    }
}

@Composable
private fun RoomFormDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by rememberSaveable(initialName, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialName))
    }
    var description by rememberSaveable(initialDescription, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialDescription))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name.text, description.text.takeIf { it.isNotBlank() })
                },
                enabled = name.text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

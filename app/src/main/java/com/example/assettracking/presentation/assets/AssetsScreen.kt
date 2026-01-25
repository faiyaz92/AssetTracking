@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.assets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.example.assettracking.presentation.tabs.model.GroupingMode
import com.example.assettracking.presentation.tabs.model.GroupedItem
import com.example.assettracking.presentation.tabs.model.SubGroup
import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.domain.model.LocationSummary
import com.example.assettracking.presentation.tabs.model.AssetListEvent
import com.example.assettracking.presentation.tabs.viewmodel.AssetListViewModel
import com.example.assettracking.util.C72RfidReader
import com.example.assettracking.util.printBarcode
import com.example.assettracking.util.rememberBarcodeImage
import java.util.ArrayDeque
import com.example.assettracking.util.RfidHardwareException

@Composable
fun AssetsScreen(
    onBack: () -> Unit,
    onAssetClick: (Long) -> Unit = {},
    viewModel: AssetListViewModel = hiltViewModel(),
    c72RfidReader: C72RfidReader = C72RfidReader(LocalContext.current)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        val message = state.message?.text
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(AssetListEvent.ClearMessage)
        }
    }

    var showAssetDialog by rememberSaveable { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<AssetSummary?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var assetToDelete by remember { mutableStateOf<AssetSummary?>(null) }

    // RFID write related state
    var showRfidDialog by remember { mutableStateOf(false) }
    var showRfidConfirmDialog by remember { mutableStateOf(false) }
    var rfidAssetToWrite by remember { mutableStateOf<AssetSummary?>(null) }
    var existingRfidData by remember { mutableStateOf<String?>(null) }

    // Error dialog state
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDetails by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Permission handling for Bluetooth printing
    var pendingPrintAsset by remember { mutableStateOf<AssetSummary?>(null) }

    // Location filter dialog state
    var showCurrentLocationDialog by remember { mutableStateOf(false) }
    var showBaseLocationDialog by remember { mutableStateOf(false) }
    var locationSearchQuery by remember { mutableStateOf("") }

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        val assetToPrint = pendingPrintAsset
        if (allGranted && assetToPrint != null) {
            printBarcode(context, assetToPrint.id.toString().padStart(6, '0'), assetToPrint.name)
            pendingPrintAsset = null
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
                            "Asset Management",
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
            FloatingActionButton(
                onClick = {
                    editingAsset = null
                    showAssetDialog = true
                },
                containerColor = Color(0xFF06B6D4), // Match teal theme
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add asset")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Professional Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { query ->
                    viewModel.onEvent(AssetListEvent.UpdateSearch(query))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                label = { Text("Search Assets") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Status filter
            var statusMenuExpanded by remember { mutableStateOf(false) }
            val statusOptions = listOf("At Home", "Deployed", "Missing", "Not Assigned")
            ExposedDropdownMenuBox(
                expanded = statusMenuExpanded,
                onExpandedChange = { statusMenuExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = state.statusFilter ?: "All Statuses",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = { statusMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Statuses") },
                        onClick = {
                            viewModel.onEvent(AssetListEvent.FilterByStatus(null))
                            statusMenuExpanded = false
                        }
                    )
                    statusOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.onEvent(AssetListEvent.FilterByStatus(option))
                                statusMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Summary count
            Text(
                text = "Total assets: ${state.filteredAssets.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
            )

            // Grouping Mode Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.groupingMode == GroupingMode.NONE,
                    onClick = { viewModel.onEvent(AssetListEvent.ChangeGroupingMode(GroupingMode.NONE)) },
                    label = { Text("No Grouping") }
                )
                FilterChip(
                    selected = state.groupingMode == GroupingMode.BY_BASE_LOCATION,
                    onClick = { viewModel.onEvent(AssetListEvent.ChangeGroupingMode(GroupingMode.BY_BASE_LOCATION)) },
                    label = { Text("Group by Base Location") }
                )
                FilterChip(
                    selected = state.groupingMode == GroupingMode.BY_CURRENT_LOCATION,
                    onClick = { viewModel.onEvent(AssetListEvent.ChangeGroupingMode(GroupingMode.BY_CURRENT_LOCATION)) },
                    label = { Text("Group by Current Location") }
                )
            }

            // Location Filters with Dialogs
            if (state.rooms.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Current Location Filter
                    Box(modifier = Modifier.weight(1f).clickable { showCurrentLocationDialog = true }) {
                        OutlinedTextField(
                            value = state.rooms.find { it.id == state.currentLocationFilter }?.name ?: "All Current Locations",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Current Location") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search locations",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            enabled = false
                        )
                    }

                    // Base Location Filter
                    Box(modifier = Modifier.weight(1f).clickable { showBaseLocationDialog = true }) {
                        OutlinedTextField(
                            value = state.rooms.find { it.id == state.baseLocationFilter }?.name ?: "All Base Locations",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Base Location") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search locations",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            enabled = false
                        )
                    }
                }
            }
            when {
                state.isLoading -> LoadingState(Modifier.fillMaxSize())
                state.filteredAssets.isEmpty() -> EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    message = if (state.searchQuery.isBlank()) {
                        "No assets yet. Tap + to add one."
                    } else {
                        "No assets found for '${state.searchQuery}'."
                    }
                )
                else -> {
                    when (state.groupingMode) {
                        GroupingMode.NONE -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.filteredAssets, key = { it.id }) { asset ->
                                    AssetCard(
                                        asset = asset,
                                        onClick = { onAssetClick(asset.id) },
                                        onEdit = {
                                            editingAsset = asset
                                            showAssetDialog = true
                                        },
                                        onDelete = {
                                            assetToDelete = asset
                                            showDeleteDialog = true
                                        },
                                        onPrint = {
                                            val allGranted = bluetoothPermissions.all { permission ->
                                                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                                            }
                                            if (allGranted) {
                                                printBarcode(context, asset.id.toString().padStart(6, '0'), asset.name)
                                            } else {
                                                pendingPrintAsset = asset
                                                permissionLauncher.launch(bluetoothPermissions)
                                            }
                                        },
                                        onRfidWrite = {
                                            rfidAssetToWrite = asset
                                            showRfidDialog = true
                                        }
                                    )
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.groupedData, key = { it.location?.id ?: -1 }) { group ->
                                    GroupedCard(
                                        group = group,
                                        groupingMode = state.groupingMode,
                                        onAssetClick = onAssetClick,
                                        onEdit = { asset ->
                                            editingAsset = asset
                                            showAssetDialog = true
                                        },
                                        onDelete = { asset ->
                                            assetToDelete = asset
                                            showDeleteDialog = true
                                        },
                                        onPrint = { asset ->
                                            val allGranted = bluetoothPermissions.all { permission ->
                                                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                                            }
                                            if (allGranted) {
                                                printBarcode(context, asset.id.toString().padStart(6, '0'), asset.name)
                                            } else {
                                                pendingPrintAsset = asset
                                                permissionLauncher.launch(bluetoothPermissions)
                                            }
                                        },
                                        onRfidWrite = { asset ->
                                            rfidAssetToWrite = asset
                                            showRfidDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Location Filter Dialogs
    if (showCurrentLocationDialog) {
        LocationFilterDialog(
            title = "Select Current Location",
            locations = state.rooms,
            searchQuery = locationSearchQuery,
            onSearchQueryChange = { locationSearchQuery = it },
            onLocationSelected = { locationId ->
                viewModel.onEvent(AssetListEvent.FilterByCurrentLocation(locationId))
                showCurrentLocationDialog = false
                locationSearchQuery = ""
            },
            onDismiss = {
                showCurrentLocationDialog = false
                locationSearchQuery = ""
            }
        )
    }

    if (showBaseLocationDialog) {
        LocationFilterDialog(
            title = "Select Base Location",
            locations = state.rooms,
            searchQuery = locationSearchQuery,
            onSearchQueryChange = { locationSearchQuery = it },
            onLocationSelected = { locationId ->
                viewModel.onEvent(AssetListEvent.FilterByBaseLocation(locationId))
                showBaseLocationDialog = false
                locationSearchQuery = ""
            },
            onDismiss = {
                showBaseLocationDialog = false
                locationSearchQuery = ""
            }
        )
    }

    if (showAssetDialog) {
        val asset = editingAsset
        AssetFormDialog(
            title = if (asset == null) "Add Asset" else "Edit Asset",
            initialName = asset?.name.orEmpty(),
            initialDetails = asset?.details.orEmpty(),
            initialCondition = asset?.condition.orEmpty(),
            initialBaseRoomId = asset?.baseRoomId,
            locations = state.rooms,
            onDismiss = { showAssetDialog = false },
            onConfirm = { name, details, condition, baseRoomId ->
                if (asset == null) {
                    viewModel.onEvent(AssetListEvent.CreateAsset(name, details, condition, baseRoomId))
                } else {
                    viewModel.onEvent(AssetListEvent.UpdateAsset(asset.id, name, details, condition, baseRoomId))
                }
                showAssetDialog = false
            }
        )
    }

    if (showDeleteDialog && assetToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Asset") },
            text = { 
                Text("Are you sure you want to delete '${assetToDelete?.name}'? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        assetToDelete?.let { viewModel.onEvent(AssetListEvent.DeleteAsset(it.id)) }
                        showDeleteDialog = false
                        assetToDelete = null
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
                    assetToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRfidDialog && rfidAssetToWrite != null) {
        AlertDialog(
            onDismissRequest = { 
                showRfidDialog = false
                rfidAssetToWrite = null
                existingRfidData = null
            },
            title = { Text("Write RFID Tag") },
            text = {
                Column {
                    Text("Writing RFID tag for asset: ${rfidAssetToWrite?.name}")
                    Text("Asset ID: ${rfidAssetToWrite?.id?.toString()?.padStart(6, '0')}")
                    Text("")
                    Text("Please place an RFID tag near the device.")
                    Text("The system will check if the tag already contains data.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        // Read existing tag data first
                        try {
                            val existingData = c72RfidReader.readTag()
                            existingRfidData = existingData

                            if (existingData != null) {
                                // Tag has data - show confirmation dialog
                                showRfidDialog = false
                                showRfidConfirmDialog = true
                            } else {
                                // Tag is blank - write directly
                                val success = c72RfidReader.writeTag(rfidAssetToWrite!!.id.toString().padStart(6, '0'))
                                if (success) {
                                    snackbarHostState.showSnackbar("RFID tag written successfully")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to write RFID tag")
                                }
                                showRfidDialog = false
                                rfidAssetToWrite = null
                            }
                        } catch (e: RfidHardwareException) {
                            val errorMessage = when {
                                e.message?.contains("not found") == true -> "RFID hardware not detected. Please ensure you're using a device with RFID capabilities."
                                e.message?.contains("not initialized") == true -> "RFID hardware failed to initialize. Check device connections and try again."
                                e.message?.contains("permission") == true -> "Permission denied for RFID access. Please grant necessary permissions."
                                e.message?.contains("connection") == true -> "Failed to connect to RFID module. Check hardware connections."
                                else -> e.message ?: "RFID hardware error occurred."
                            }
                            val stackTrace = android.util.Log.getStackTraceString(e)
                            errorDetails = Pair(errorMessage, stackTrace)
                            showErrorDialog = true
                            snackbarHostState.showSnackbar(errorMessage)
                            showRfidDialog = false
                            rfidAssetToWrite = null
                        } catch (e: Exception) {
                            val errorMessage = "Unexpected error: ${e.message}"
                            val stackTrace = android.util.Log.getStackTraceString(e)
                            errorDetails = Pair(errorMessage, stackTrace)
                            showErrorDialog = true
                            snackbarHostState.showSnackbar(errorMessage)
                            showRfidDialog = false
                            rfidAssetToWrite = null
                        }
                    }
                }) {
                    Text("Write Tag")
                }
            },
            dismissButton = {
                Row {
                    if (errorDetails != null) {
                        TextButton(onClick = { showErrorDialog = true }) {
                            Text("Show Error Details")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = { 
                        showRfidDialog = false
                        rfidAssetToWrite = null
                        existingRfidData = null
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showRfidConfirmDialog && rfidAssetToWrite != null && existingRfidData != null) {
        val isAssetId = existingRfidData == rfidAssetToWrite!!.id.toString().padStart(6, '0')
        
        AlertDialog(
            onDismissRequest = { 
                showRfidConfirmDialog = false
                rfidAssetToWrite = null
                existingRfidData = null
            },
            title = { Text("RFID Tag Already Has Data") },
            text = {
                Column {
                    if (isAssetId) {
                        Text("This RFID tag is already attached to this asset!")
                        Text("Asset ID: $existingRfidData")
                    } else {
                        Text("This RFID tag contains data: $existingRfidData")
                        Text("")
                        Text("Do you want to overwrite it with this asset's ID?")
                        Text("Asset: ${rfidAssetToWrite?.name}")
                        Text("New ID: ${rfidAssetToWrite?.id?.toString()?.padStart(6, '0')}")
                    }
                }
            },
            confirmButton = {
                if (!isAssetId) {
                    Button(onClick = {
                        coroutineScope.launch {
                            try {
                                // Write directly to overwrite existing data (matching demo pattern)
                                // No need to kill tag - writeData overwrites automatically
                                val success = c72RfidReader.writeTag(rfidAssetToWrite!!.id.toString().padStart(6, '0'))
                                if (success) {
                                    snackbarHostState.showSnackbar("RFID tag overwritten successfully")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to write RFID tag")
                                }
                            } catch (e: RfidHardwareException) {
                                val errorMessage = when {
                                    e.message?.contains("not found") == true -> "RFID hardware not detected. Please ensure you're using a device with RFID capabilities."
                                    e.message?.contains("not initialized") == true -> "RFID hardware failed to initialize. Check device connections and try again."
                                    e.message?.contains("permission") == true -> "Permission denied for RFID access. Please grant necessary permissions."
                                    e.message?.contains("connection") == true -> "Failed to connect to RFID module. Check hardware connections."
                                    else -> e.message ?: "RFID hardware error occurred."
                                }
                                val stackTrace = android.util.Log.getStackTraceString(e)
                                errorDetails = Pair(errorMessage, stackTrace)
                                showErrorDialog = true
                                snackbarHostState.showSnackbar(errorMessage)
                            } catch (e: Exception) {
                                val errorMessage = "Unexpected error: ${e.message}"
                                val stackTrace = android.util.Log.getStackTraceString(e)
                                errorDetails = Pair(errorMessage, stackTrace)
                                showErrorDialog = true
                                snackbarHostState.showSnackbar(errorMessage)
                            } finally {
                                showRfidConfirmDialog = false
                                rfidAssetToWrite = null
                                existingRfidData = null
                            }
                        }
                    }) {
                        Text("Overwrite")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRfidConfirmDialog = false
                    rfidAssetToWrite = null
                    existingRfidData = null
                }) {
                    Text(if (isAssetId) "OK" else "Cancel")
                }
            }
        )
    }

    // Error Dialog
    if (showErrorDialog && errorDetails != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error Details") },
            text = {
                Column {
                    Text("An error occurred during RFID operation:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorDetails!!.first,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Full Stack Trace:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorDetails!!.second,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .height(200.dp)
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Error Details", "${errorDetails!!.first}\n\nStack Trace:\n${errorDetails!!.second}")
                        clipboard.setPrimaryClip(clip)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Error details copied to clipboard")
                        }
                    }) {
                        Text("Copy Details")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Error Dialog
    if (showErrorDialog && errorDetails != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error Details") },
            text = {
                Column {
                    Text("An error occurred during RFID operation:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorDetails!!.first,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Full Stack Trace:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorDetails!!.second,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .height(200.dp)
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Error Details", "${errorDetails!!.first}\n\nStack Trace:\n${errorDetails!!.second}")
                        clipboard.setPrimaryClip(clip)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Error details copied to clipboard")
                        }
                    }) {
                        Text("Copy Details")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

// ---- Helpers ----
@Composable
private fun LocationFilterDialog(
    title: String,
    locations: List<LocationSummary>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onLocationSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredLocations = locations.filter { location ->
        val query = searchQuery.lowercase()
        location.name.lowercase().contains(query) ||
        location.id.toString().contains(query) ||
        (location.locationCode.lowercase().contains(query))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search by name, code, or ID") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        TextButton(
                            onClick = { onLocationSelected(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "All Locations",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(filteredLocations, key = { it.id }) { location ->
                        TextButton(
                            onClick = { onLocationSelected(location.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    location.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "ID: ${location.id}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Code: ${location.locationCode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AssetCardContent(
    asset: AssetSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit,
    onRfidWrite: () -> Unit
) {
    val barcodeBitmap = rememberBarcodeImage(content = asset.id.toString(), width = 800, height = 220)
    Column(modifier = Modifier.padding(20.dp)) {
        // Asset Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Code: ${asset.id.toString().padStart(6, '0')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (asset.currentRoomId == asset.baseRoomId) Color.Green else Color(0xFFFFA500))
            )
        }

        // Asset Details
        asset.details?.takeIf { it.isNotBlank() }?.let { details ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Asset Condition
        asset.condition?.takeIf { it.isNotBlank() }?.let { condition ->
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "C",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = condition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Asset Status
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (asset.status) {
                            "At Home" -> Color(0xFF10B981).copy(alpha = 0.1f) // Green
                            "Deployed" -> Color(0xFFF59E0B).copy(alpha = 0.1f) // Orange
                            "Missing" -> Color(0xFFEF4444).copy(alpha = 0.1f) // Red
                            "Not Assigned" -> Color(0xFF3B82F6).copy(alpha = 0.1f) // Blue
                            else -> Color.Gray.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (asset.status) {
                            "At Home" -> Color(0xFF10B981) // Green
                            "Deployed" -> Color(0xFFF59E0B) // Orange
                            "Missing" -> Color(0xFFEF4444) // Red
                            "Not Assigned" -> Color(0xFF3B82F6) // Blue
                            else -> Color.Gray
                        }
                    )
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = asset.status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Location Info
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = buildString {
                    append("Base: ")
                    append(asset.baseRoomName ?: "Unassigned")
                    if (asset.currentRoomName != null && asset.currentRoomName != asset.baseRoomName) {
                        append(" | Current: ${asset.currentRoomName}")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Barcode Image
        barcodeBitmap?.let { bitmap ->
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Barcode for ${asset.id.toString().padStart(6, '0')}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(8.dp)
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
            IconButton(
                onClick = onRfidWrite,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner, // Using QrCodeScanner as RFID icon
                    contentDescription = "Write RFID tag",
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
                    contentDescription = "Edit asset",
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
                    contentDescription = "Delete asset",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun AssetCard(
    asset: AssetSummary,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit,
    onRfidWrite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        AssetCardContent(asset, onEdit, onDelete, onPrint, onRfidWrite)
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
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
private fun AssetFormDialog(
    title: String,
    initialName: String,
    initialDetails: String,
    initialCondition: String,
    initialBaseRoomId: Long?,
    locations: List<LocationSummary>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, Long?) -> Unit
) {
    val name = rememberSaveable(initialName) { mutableStateOf(initialName) }
    val details = rememberSaveable(initialDetails) { mutableStateOf(initialDetails) }
    val condition = rememberSaveable(initialCondition) { mutableStateOf(initialCondition) }
    val baseRoomId = remember { mutableStateOf(initialBaseRoomId) }

    // Hierarchical location dialog state
    val (showLocationDialog, setShowLocationDialog) = remember { mutableStateOf(false) }
    val (locationSearchQuery, setLocationSearchQuery) = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name.value,
                    onValueChange = { name.value = it },
                    label = { Text("Asset name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = details.value,
                    onValueChange = { details.value = it },
                    label = { Text("Details (optional)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = condition.value,
                    onValueChange = { condition.value = it },
                    label = { Text("Condition (optional)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
                
                // Base location selection with hierarchical dialog
                OutlinedTextField(
                    value = locations.find { it.id == baseRoomId.value }?.name ?: "No base location",
                    onValueChange = {},
                    label = { Text("Base location (optional)") },
                    readOnly = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Select location",
                            modifier = Modifier.rotate(270f), // Rotate to point down
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { setShowLocationDialog(true) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name.value,
                        details.value.takeIf { it.isNotBlank() },
                        condition.value.takeIf { it.isNotBlank() },
                        baseRoomId.value
                    )
                },
                enabled = name.value.isNotBlank()
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

    // Hierarchical location selection dialog
    if (showLocationDialog) {
        HierarchicalLocationDialog(
            title = "Select Base Location",
            locations = locations,
            searchQuery = locationSearchQuery,
            onSearchQueryChange = setLocationSearchQuery,
            onLocationSelected = { locationId ->
                baseRoomId.value = locationId
                setShowLocationDialog(false)
                setLocationSearchQuery("")
            },
            onDismiss = {
                setShowLocationDialog(false)
                setLocationSearchQuery("")
            }
        )
    }
}

@Composable
private fun HierarchicalLocationItem(
    location: LocationSummary,
    allLocations: List<LocationSummary>,
    locationMap: Map<Long, LocationSummary>,
    onLocationSelected: (Long) -> Unit,
    level: Int
) {
    val children = allLocations.filter { it.parentId == location.id }

    Column {
        TextButton(
            onClick = { onLocationSelected(location.id) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indentation for hierarchy
                    Spacer(modifier = Modifier.width((level * 24).dp))

                    // Location icon
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            location.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "ID: ${location.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Code: ${location.locationCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Children indicator
                    if (location.hasChildren) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Has children",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Render children
        children.forEach { child ->
            HierarchicalLocationItem(
                location = child,
                allLocations = allLocations,
                locationMap = locationMap,
                onLocationSelected = onLocationSelected,
                level = level + 1
            )
        }
    }
}

@Composable
private fun HierarchicalLocationDialog(
    title: String,
    locations: List<LocationSummary>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onLocationSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    // Build hierarchical structure
    val locationMap = locations.associateBy { it.id }
    val rootLocations = locations.filter { it.parentId == null }

    // Function to get all descendants of a location
    fun getAllDescendants(locationId: Long): List<LocationSummary> {
        val result = mutableListOf<LocationSummary>()
        val queue = ArrayDeque<Long>()
        queue.add(locationId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            locations.filter { it.parentId == currentId }.forEach { child ->
                result.add(child)
                queue.add(child.id)
            }
        }
        return result
    }

    // Filter locations based on search query
    val filteredLocations = if (searchQuery.isBlank()) {
        rootLocations
    } else {
        val query = searchQuery.lowercase()
        locations.filter { location ->
            location.name.lowercase().contains(query) ||
            location.id.toString().contains(query) ||
            location.locationCode.lowercase().contains(query)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search by name, code, or ID") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier.height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "No base location" option
                    item {
                        TextButton(
                            onClick = { onLocationSelected(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "No base location",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Hierarchical location items
                    items(filteredLocations, key = { it.id }) { location ->
                        HierarchicalLocationItem(
                            location = location,
                            allLocations = locations,
                            locationMap = locationMap,
                            onLocationSelected = onLocationSelected,
                            level = 0
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GroupedCard(
    group: GroupedItem,
    groupingMode: GroupingMode,
    onAssetClick: (Long) -> Unit,
    onEdit: (AssetSummary) -> Unit,
    onDelete: (AssetSummary) -> Unit,
    onPrint: (AssetSummary) -> Unit,
    onRfidWrite: (AssetSummary) -> Unit
) {
    val groupTitle = group.location?.name ?: if (groupingMode == GroupingMode.BY_BASE_LOCATION) "Unassigned" else "Missing"
    val groupCode = group.location?.locationCode

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    groupCode?.let {
                        Text(
                            text = "Code: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "${group.assets.size} assets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Divider()

            group.subGroups.forEach { sub ->
                val subTitle = when (groupingMode) {
                    GroupingMode.BY_BASE_LOCATION -> "Current: ${sub.location?.name ?: "Not Assigned"}"
                    GroupingMode.BY_CURRENT_LOCATION -> "Base: ${sub.location?.name ?: "Unassigned"}"
                    GroupingMode.NONE -> sub.location?.name ?: "Unknown"
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${sub.assets.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sub.assets.forEach { asset ->
                            AssetCard(
                                asset = asset,
                                onClick = { onAssetClick(asset.id) },
                                onEdit = { onEdit(asset) },
                                onDelete = { onDelete(asset) },
                                onPrint = { onPrint(asset) },
                                onRfidWrite = { onRfidWrite(asset) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// End of file helper




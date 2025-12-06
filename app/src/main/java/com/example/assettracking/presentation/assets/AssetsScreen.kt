@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.assets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedTextField
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
import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.domain.model.LocationSummary
import com.example.assettracking.presentation.tabs.model.AssetListEvent
import com.example.assettracking.presentation.tabs.viewmodel.AssetListViewModel
import com.example.assettracking.util.C72RfidReader
import com.example.assettracking.util.printBarcode
import com.example.assettracking.util.rememberBarcodeImage
import com.example.assettracking.util.RfidHardwareException

@Composable
fun AssetsScreen(
    onBack: () -> Unit,
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
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filteredAssets, key = { it.id }) { asset ->
                        AssetCard(
                            asset = asset,
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
        }
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
                    viewModel.onEvent(AssetListEvent.UpdateAsset(asset.id, name, details, condition))
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
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("RFID tag written successfully")
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Failed to write RFID tag")
                                }
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
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(errorMessage)
                        }
                        showRfidDialog = false
                        rfidAssetToWrite = null
                    } catch (e: Exception) {
                        val errorMessage = "Unexpected error: ${e.message}"
                        val stackTrace = android.util.Log.getStackTraceString(e)
                        errorDetails = Pair(errorMessage, stackTrace)
                        showErrorDialog = true
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(errorMessage)
                        }
                        showRfidDialog = false
                        rfidAssetToWrite = null
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
                        try {
                            // Kill existing data and write new data
                            val killed = c72RfidReader.killTag("00000000") // Default password
                            if (killed) {
                                val success = c72RfidReader.writeTag(rfidAssetToWrite!!.id.toString().padStart(6, '0'))
                                if (success) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("RFID tag overwritten successfully")
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Failed to write RFID tag")
                                    }
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Failed to clear existing RFID data")
                                }
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
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(errorMessage)
                            }
                        } catch (e: Exception) {
                            val errorMessage = "Unexpected error: ${e.message}"
                            val stackTrace = android.util.Log.getStackTraceString(e)
                            errorDetails = Pair(errorMessage, stackTrace)
                            showErrorDialog = true
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(errorMessage)
                            }
                        }
                        showRfidConfirmDialog = false
                        rfidAssetToWrite = null
                        existingRfidData = null
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
@Composable
private fun AssetCard(
    asset: AssetSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit,
    onRfidWrite: () -> Unit
) {
    val barcodeBitmap = rememberBarcodeImage(content = asset.id.toString(), width = 800, height = 220)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
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
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
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
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No base location") },
                            onClick = {
                                baseRoomId.value = null
                                expanded = false
                            }
                        )
                        locations.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(location.name) },
                                onClick = {
                                    baseRoomId.value = location.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
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
}

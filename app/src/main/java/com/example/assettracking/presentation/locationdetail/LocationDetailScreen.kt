@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.locationdetail

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.QrCode
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.domain.model.LocationSummary
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assettracking.util.rememberBarcodeImage

@Composable
fun LocationDetailScreen(
    viewModel: LocationDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    navController: androidx.navigation.NavHostController
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }
    val (assetToDelete, setAssetToDelete) = remember { mutableStateOf<AssetSummary?>(null) }

    val (showRfidDialog, setShowRfidDialog) = remember { mutableStateOf(false) }
    val (showErrorDialog, setShowErrorDialog) = remember { mutableStateOf(false) }
    val (errorDetails, setErrorDetails) = remember { mutableStateOf<Pair<String, String>?>(null) }

    // Add child location dialog state
    val (showAddChildDialog, setShowAddChildDialog) = remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            // Store error details for the error dialog
            setErrorDetails(Pair(message.text, message.stackTrace))
            snackbarHostState.showSnackbar(message.text)
            viewModel.clearMessage()
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        val contents = result.contents
        if (contents != null) {
            viewModel.assignAsset(contents)
        }
    }

    val scanOptions = remember {
        ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.CODE_128)
            setPrompt("Scan asset barcode")
            setCameraId(0)
            setBeepEnabled(true)
            setOrientationLocked(true)
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
                            state.locationDetail?.name ?: "Location",
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Add Child Location Button
                FloatingActionButton(
                    onClick = { setShowAddChildDialog(true) },
                    containerColor = Color(0xFF06B6D4), // Teal color
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Place, contentDescription = "Add child location")
                }

                // Bulk RFID Scan Button
                FloatingActionButton(
                    onClick = {
                        viewModel.startBulkRfidScan()
                        setShowRfidDialog(true)
                    },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(imageVector = Icons.Default.Inventory2, contentDescription = "Bulk RFID Scan")
                }

                // Single RFID Scan Button
                FloatingActionButton(
                    onClick = {
                        viewModel.startSingleRfidScan()
                        setShowRfidDialog(true)
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.RssFeed, contentDescription = "Single RFID Scan")
                }

                // Barcode Scan Button
                FloatingActionButton(
                    onClick = { scannerLauncher.launch(scanOptions) },
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
                }
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingState(modifier = Modifier.padding(paddingValues).fillMaxSize())
            state.locationDetail == null -> EmptyState(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                message = "Location not found"
            )
            else -> {
                val detail = state.locationDetail
                if (detail != null) {
                    LocationDetailContent(
                        modifier = Modifier.padding(paddingValues).fillMaxSize(),
                        locationName = detail.name,
                        locationDescription = detail.description,
                        locationCode = detail.locationCode,
                        children = detail.children,
                        assets = detail.assets,
                        isGrouped = state.isGrouped,
                        onToggleGrouping = { viewModel.toggleGrouping() },
                        locationId = detail.id,
                        onDetach = { asset ->
                            setAssetToDelete(asset)
                            setShowDeleteDialog(true)
                        },
                        onNavigateToChild = { childId ->
                            // Navigate to child location detail
                            navController.navigate("location_detail/$childId")
                        },
                        onEditChild = { childId ->
                            // TODO: Implement edit child location
                        },
                        onDeleteChild = { childId ->
                            // TODO: Implement delete child location
                        },
                        onPrintChild = { childId ->
                            // TODO: Implement print child location barcode
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog && assetToDelete != null) {
        AlertDialog(
            onDismissRequest = { setShowDeleteDialog(false) },
            title = { Text("Detach Asset") },
            text = { 
                Text("Are you sure you want to detach '${assetToDelete?.name}' from this location? The asset will be unassigned from its current location.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        assetToDelete?.let { viewModel.detachAsset(it.id) }
                        setShowDeleteDialog(false)
                        setAssetToDelete(null)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Detach")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    setShowDeleteDialog(false)
                    setAssetToDelete(null)
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRfidDialog) {
        AlertDialog(
            onDismissRequest = {
                setShowRfidDialog(false)
                viewModel.clearRfidScan()
            },
            title = { 
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RFID Scan")
                    if (state.isRfidScanning || state.scanCompleted) {
                        Text(
                            "${state.totalScanned} found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (state.isRfidScanning) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scanning... ${state.totalScanned} tag(s) detected")
                            }
                            if (state.isScanStoppable) {
                                Text(
                                    "Tap 'Stop' when you're done, or scan will auto-complete in ~30 seconds",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.scannedRfidTags.isNotEmpty()) {
                                Text(
                                    "Recent tags:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                state.scannedRfidTags.takeLast(3).forEach { tag ->
                                    Text(
                                        "• $tag",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    } else if (state.scanCompleted && state.scannedRfidTags.isEmpty()) {
                        Column {
                            Text(
                                "✓ Scan completed",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No RFID tags found in range. Try scanning again or move closer to tags.")
                            if (errorDetails != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { setShowErrorDialog(true) }) {
                                    Text("Show Error Details")
                                }
                            }
                        }
                    } else if (state.scannedRfidTags.isEmpty()) {
                        Column {
                            Text("No RFID tags found. Try scanning again.")
                            if (errorDetails != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { setShowErrorDialog(true) }) {
                                    Text("Show Error Details")
                                }
                            }
                        }
                    } else {
                        Column {
                            // Scan completed header
                            if (state.scanCompleted) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "✓",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            "Scan Complete",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Found ${state.totalScanned} unique tag${if (state.totalScanned != 1) "s" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (state.scannedRfidTags.size > 1) 
                                        "Select tags to assign:" 
                                    else 
                                        "Tag found:",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (state.scannedRfidTags.size > 1) {
                                    Button(
                                        onClick = {
                                            viewModel.assignBulkAssets(state.scannedRfidTags)
                                            setShowRfidDialog(false)
                                            viewModel.clearRfidScan()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("Assign All Valid")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (state.scannedRfidTags.size > 1) 
                                    "✓ Valid tags will be assigned  ✗ Unknown tags will be ignored" 
                                else 
                                    "Tap to assign this tag:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            state.scannedRfidTags.forEach { tag ->
                                val assetId = tag.toLongOrNull()
                                val isValidFormat = assetId != null
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isValidFormat) 
                                                MaterialTheme.colorScheme.surfaceVariant 
                                            else 
                                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                        )
                                        .clickable(enabled = isValidFormat) {
                                            if (isValidFormat) {
                                                viewModel.assignAsset(tag)
                                                setShowRfidDialog(false)
                                                viewModel.clearRfidScan()
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = false,
                                        onClick = {
                                            if (isValidFormat) {
                                                viewModel.assignAsset(tag)
                                                setShowRfidDialog(false)
                                                viewModel.clearRfidScan()
                                            }
                                        },
                                        enabled = isValidFormat
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "EPC: $tag",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (isValidFormat) {
                                            Text(
                                                text = "Asset ID: " + (assetId?.toString()?.padStart(6, '0') ?: ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Text(
                                                text = "⚠ Invalid format - will be ignored",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (state.isRfidScanning && state.isScanStoppable) {
                    // Show Stop button during bulk scan
                    Button(
                        onClick = {
                            viewModel.stopBulkRfidScan()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Stop Scanning")
                    }
                } else if (!state.isRfidScanning) {
                    TextButton(onClick = {
                        setShowRfidDialog(false)
                        viewModel.clearRfidScan()
                    }) {
                        Text("Close")
                    }
                } else {
                    // No button while single scan is processing
                }
            }
        )
    }

    // Error Details Dialog
    if (showErrorDialog && errorDetails != null) {
        val (message, stackTrace) = errorDetails
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { setShowErrorDialog(false) },
            title = { Text("Error Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Error Message:", style = MaterialTheme.typography.titleSmall)
                    Text(message, style = MaterialTheme.typography.bodyMedium)

                    if (stackTrace.isNotEmpty()) {
                        Text("Stack Trace:", style = MaterialTheme.typography.titleSmall)
                        Text(
                            stackTrace,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .verticalScroll(rememberScrollState())
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Error Details", "Message: $message\n\nStack Trace:\n$stackTrace")
                        clipboard.setPrimaryClip(clip)
                    }) {
                        Text("Copy")
                    }
                    TextButton(onClick = { setShowErrorDialog(false) }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Add Child Location Dialog
    if (showAddChildDialog) {
        val childName = remember { mutableStateOf("") }
        val childDescription = remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { setShowAddChildDialog(false) },
            title = { Text("Add Child Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = childName.value,
                        onValueChange = { childName.value = it },
                        label = { Text("Location name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                    OutlinedTextField(
                        value = childDescription.value,
                        onValueChange = { childDescription.value = it },
                        label = { Text("Description (optional)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
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
                        if (childName.value.isNotBlank()) {
                            viewModel.createChildLocation(
                                childName.value.trim(),
                                childDescription.value.takeIf { it.isNotBlank() }
                            )
                            setShowAddChildDialog(false)
                        }
                    },
                    enabled = childName.value.isNotBlank()
                ) {
                    Text("Add Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { setShowAddChildDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LocationDetailContent(
    modifier: Modifier,
    locationName: String,
    locationDescription: String?,
    locationCode: String,
    children: List<LocationSummary>,
    assets: List<AssetSummary>,
    isGrouped: Boolean,
    onToggleGrouping: () -> Unit,
    locationId: Long,
    onDetach: (AssetSummary) -> Unit,
    onNavigateToChild: (Long) -> Unit,
    onEditChild: (Long) -> Unit,
    onDeleteChild: (Long) -> Unit,
    onPrintChild: (Long) -> Unit
) {
    val showBarcode = remember { mutableStateOf(false) }
    val barcodeBitmap = rememberBarcodeImage(content = locationId.toString().padStart(6, '0'), width = 800, height = 220)

    Column(modifier = modifier) {
        // Professional Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            locationName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Code: $locationCode",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        locationDescription?.takeIf { it.isNotBlank() }?.let { description ->
                            Text(
                                description,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            if (children.isNotEmpty()) "Manage child locations" else "Manage assets in this location",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                    // Barcode toggle icon
                    IconButton(
                        onClick = { showBarcode.value = !showBarcode.value },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = if (showBarcode.value) "Hide barcode" else "Show barcode",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Barcode Display Section
        if (showBarcode.value && barcodeBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Location Barcode",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = barcodeBitmap,
                            contentDescription = "Location barcode for $locationName",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ID: ${locationId.toString().padStart(6, '0')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            if (children.isNotEmpty()) {
                // Show child locations
                Text(
                    text = "Child Locations (${children.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                ChildLocationList(
                    children = children,
                    onNavigateToChild = onNavigateToChild,
                    onEditChild = onEditChild,
                    onDeleteChild = onDeleteChild,
                    onPrintChild = onPrintChild
                )
            } else {
                // Show assets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assets (${assets.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = onToggleGrouping) {
                        Text(if (isGrouped) "Ungroup" else "Group by Base Location")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (assets.isEmpty()) {
                    EmptyState(
                        modifier = Modifier.fillMaxSize(),
                        message = "No assets linked yet. Tap the scan button to add one."
                    )
                } else {
                    if (isGrouped) {
                        GroupedAssetList(assets = assets, locationId = locationId, onDetach = onDetach)
                    } else {
                        FlatAssetList(assets = assets, onDetach = onDetach)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildLocationList(
    children: List<LocationSummary>,
    onNavigateToChild: (Long) -> Unit,
    onEditChild: (Long) -> Unit,
    onDeleteChild: (Long) -> Unit,
    onPrintChild: (Long) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(children) { child ->
            ChildLocationCard(
                location = child,
                onClick = { onNavigateToChild(child.id) },
                onEdit = { onEditChild(child.id) },
                onDelete = { onDeleteChild(child.id) },
                onPrint = { onPrintChild(child.id) }
            )
        }
    }
}

@Composable
private fun ChildLocationCard(
    location: LocationSummary,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit
) {
    val barcodeBitmap = rememberBarcodeImage(content = location.id.toString().padStart(6, '0'), width = 800, height = 220)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Location Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    location.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (location.hasChildren) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Has children",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Barcode Image
            barcodeBitmap?.let { bitmap ->
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap,
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
private fun AssetInLocationCard(
    asset: AssetSummary,
    onDetach: () -> Unit
) {
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
                // Asset icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF059669),
                                    Color(0xFF10B981)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
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
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E40AF).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location",
                        tint = Color(0xFF1E40AF),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Base: ${asset.baseRoomName ?: "Unassigned"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Button
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDetach,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Detach asset",
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
private fun FlatAssetList(
    assets: List<AssetSummary>,
    onDetach: (AssetSummary) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(assets, key = { it.id }) { asset ->
            AssetInLocationCard(asset = asset, onDetach = { onDetach(asset) })
        }
    }
}

@Composable
private fun GroupedAssetList(
    assets: List<AssetSummary>,
    locationId: Long,
    onDetach: (AssetSummary) -> Unit
) {
    val grouped = assets.groupBy { it.baseRoomId }
    val sortedKeys = grouped.keys.sortedWith(compareBy<Long?> { if (it == locationId) 0 else 1 }.thenBy { it ?: Long.MAX_VALUE })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (baseRoomId in sortedKeys) {
            val groupAssets = grouped[baseRoomId] ?: emptyList()
            val headerText = if (baseRoomId == locationId) {
                "This Location's Assets"
            } else {
                val roomName = groupAssets.firstOrNull()?.baseRoomName ?: "Unassigned"
                "$roomName Assets"
            }
            items(listOf(headerText)) { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(groupAssets, key = { it.id }) { asset ->
                AssetInLocationCard(asset = asset, onDetach = { onDetach(asset) })
            }
        }
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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.locationdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.domain.model.LocationSummary
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assettracking.util.rememberBarcodeImage
import com.example.assettracking.util.printBarcode

private data class FabAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val containerColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun FabMenuButton(action: FabAction) {
    ExtendedFloatingActionButton(
        onClick = action.onClick,
        containerColor = action.containerColor,
        contentColor = Color.White,
        text = { Text(action.label) },
        icon = { Icon(action.icon, contentDescription = action.label) }
    )
}

@Composable
fun LocationDetailScreen(
    viewModel: LocationDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    navController: androidx.navigation.NavHostController
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }
    val (assetToDelete, setAssetToDelete) = remember { mutableStateOf<AssetSummary?>(null) }
    val (showAddAssetDialog, setShowAddAssetDialog) = remember { mutableStateOf(false) }

    val (showRfidDialog, setShowRfidDialog) = remember { mutableStateOf(false) }
    val (showErrorDialog, setShowErrorDialog) = remember { mutableStateOf(false) }
    val (errorDetails, setErrorDetails) = remember { mutableStateOf<Pair<String, String>?>(null) }

    // Child location dialog state
    val (showAddChildDialog, setShowAddChildDialog) = remember { mutableStateOf(false) }
    val (showEditChildDialog, setShowEditChildDialog) = remember { mutableStateOf(false) }
    val (childToEdit, setChildToEdit) = remember { mutableStateOf<LocationSummary?>(null) }
    val (showDeleteChildDialog, setShowDeleteChildDialog) = remember { mutableStateOf(false) }
    val (childToDelete, setChildToDelete) = remember { mutableStateOf<LocationSummary?>(null) }

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

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            var fabExpanded by remember { mutableStateOf(true) }

            val fabActions = listOf(
                FabAction(
                    label = "Add Asset",
                    icon = Icons.Default.Add,
                    containerColor = MaterialTheme.colorScheme.primary,
                    onClick = { setShowAddAssetDialog(true) }
                ),
                FabAction(
                    label = "Add Child",
                    icon = Icons.Default.Place,
                    containerColor = Color(0xFF06B6D4),
                    onClick = { setShowAddChildDialog(true) }
                ),
                FabAction(
                    label = "Bulk RFID",
                    icon = Icons.Default.Inventory2,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        viewModel.startBulkRfidScan()
                        setShowRfidDialog(true)
                    }
                ),
                FabAction(
                    label = "Single RFID",
                    icon = Icons.Default.RssFeed,
                    containerColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        viewModel.startSingleRfidScan()
                        setShowRfidDialog(true)
                    }
                ),
                FabAction(
                    label = "Scan Barcode",
                    icon = Icons.Default.QrCodeScanner,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { scannerLauncher.launch(scanOptions) }
                )
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        fabActions.forEach { action ->
                            FabMenuButton(action = action)
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (fabExpanded) "Collapse menu" else "Expand menu"
                    )
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
                        onPrintAsset = { asset ->
                            printBarcode(context, asset.id.toString().padStart(6, '0'), asset.name)
                        },
                        onNavigateToChild = { childId ->
                            // Navigate to child location detail
                            navController.navigate("location_detail/$childId")
                        },
                        onEditChild = { childId ->
                            state.locationDetail?.children?.firstOrNull { it.id == childId }?.let { child ->
                                setChildToEdit(child)
                                setShowEditChildDialog(true)
                            }
                        },
                        onDeleteChild = { childId ->
                            state.locationDetail?.children?.firstOrNull { it.id == childId }?.let { child ->
                                setChildToDelete(child)
                                setShowDeleteChildDialog(true)
                            }
                        },
                        onPrintChild = { childId ->
                            state.locationDetail?.children?.firstOrNull { it.id == childId }?.let { child ->
                                printBarcode(context, child.id.toString().padStart(6, '0'), child.name)
                            }
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

    if (showAddAssetDialog) {
        var assetName by remember { mutableStateOf("") }
        var assetDetails by remember { mutableStateOf("") }
        var assetCondition by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { setShowAddAssetDialog(false) },
            title = { Text("Add Asset to this Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = assetName,
                        onValueChange = { assetName = it },
                        label = { Text("Asset name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                    OutlinedTextField(
                        value = assetDetails,
                        onValueChange = { assetDetails = it },
                        label = { Text("Details (optional)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                    OutlinedTextField(
                        value = assetCondition,
                        onValueChange = { assetCondition = it },
                        label = { Text("Condition (optional)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        "Base and current location will be preset to this location.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createAssetForLocation(
                            assetName,
                            assetDetails.takeIf { it.isNotBlank() },
                            assetCondition.takeIf { it.isNotBlank() }
                        )
                        setShowAddAssetDialog(false)
                    },
                    enabled = assetName.isNotBlank()
                ) {
                    Text("Add Asset")
                }
            },
            dismissButton = {
                TextButton(onClick = { setShowAddAssetDialog(false) }) {
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
                                        "ÔÇó $tag",
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
                                "Ô£ô Scan completed",
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
                                        "Ô£ô",
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
                                    "Ô£ô Valid tags will be assigned  Ô£ù Unknown tags will be ignored" 
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
                                                text = "ÔÜá Invalid format - will be ignored",
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

    if (showEditChildDialog && childToEdit != null) {
        val childName = remember { mutableStateOf(childToEdit!!.name) }
        val childDescription = remember { mutableStateOf(childToEdit!!.description ?: "") }

        AlertDialog(
            onDismissRequest = {
                setShowEditChildDialog(false)
                setChildToEdit(null)
            },
            title = { Text("Edit Child Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = childName.value,
                        onValueChange = { childName.value = it },
                        label = { Text("Location name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = childDescription.value,
                        onValueChange = { childDescription.value = it },
                        label = { Text("Description (optional)") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateChildLocation(
                            childToEdit!!.id,
                            childName.value,
                            childDescription.value.takeIf { it.isNotBlank() }
                        )
                        setShowEditChildDialog(false)
                        setChildToEdit(null)
                    },
                    enabled = childName.value.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    setShowEditChildDialog(false)
                    setChildToEdit(null)
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteChildDialog && childToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                setShowDeleteChildDialog(false)
                setChildToDelete(null)
            },
            title = { Text("Delete Child Location") },
            text = {
                Text("Delete '${childToDelete?.name}'? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChildLocation(childToDelete!!.id)
                        setShowDeleteChildDialog(false)
                        setChildToDelete(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    setShowDeleteChildDialog(false)
                    setChildToDelete(null)
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
    onPrintAsset: (AssetSummary) -> Unit,
    onNavigateToChild: (Long) -> Unit,
    onEditChild: (Long) -> Unit,
    onDeleteChild: (Long) -> Unit,
    onPrintChild: (Long) -> Unit
) {
    val showBarcode = remember { mutableStateOf(false) }
    val barcodeBitmap = rememberBarcodeImage(content = locationId.toString().padStart(6, '0'), width = 800, height = 220)
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val collapseRangePx = with(density) { 120.dp.toPx() }
    val collapseFraction by remember {
        derivedStateOf {
            val rawFraction = if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                listState.firstVisibleItemScrollOffset / collapseRangePx
            }
            rawFraction.coerceIn(0f, 1f)
        }
    }
    val headerHeight = lerp(164.dp, 120.dp, collapseFraction) // match Locations header max height
    val headerPadding = lerp(24.dp, 16.dp, collapseFraction) // match Locations header padding

    val currentAssets = assets.filter { it.currentRoomId == locationId }
    val belongingAssets = assets.filter { it.baseRoomId == locationId }
    val missingAssets = assets.filter { it.baseRoomId == locationId && it.currentRoomId == null }
    val hasAnyAssets = currentAssets.isNotEmpty() || belongingAssets.isNotEmpty()

    var selectedTab by remember { mutableStateOf(LocationAssetTab.CURRENT) }

    val (selectedAssets, tabTitle) = when (selectedTab) {
        LocationAssetTab.CURRENT -> currentAssets to "Current Assets"
        LocationAssetTab.BELONGING -> belongingAssets to "Belonging Assets"
        LocationAssetTab.MISSING -> missingAssets to "Missing Assets"
    }

    val currentSelfCount = currentAssets.count { it.baseRoomId == locationId }
    val currentOtherCount = currentAssets.size - currentSelfCount
    val belongingAtHome = belongingAssets.count { it.currentRoomId == locationId }
    val belongingElsewhere = belongingAssets.count { it.currentRoomId != null && it.currentRoomId != locationId }
    val missingCount = missingAssets.size

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        stickyHeader {
            // Professional Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(headerPadding)
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
        }

        if (showBarcode.value && barcodeBitmap != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(16.dp),
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

        if (children.isNotEmpty()) {
            item {
                Text(
                    text = "Child Locations (${children.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(children, key = { it.id }) { child ->
                ChildLocationCard(
                    location = child,
                    onClick = { onNavigateToChild(child.id) },
                    onEdit = { onEditChild(child.id) },
                    onDelete = { onDeleteChild(child.id) },
                    onPrint = { onPrintChild(child.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (hasAnyAssets) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$tabTitle (${selectedAssets.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onToggleGrouping) {
                        Text(if (isGrouped) "Ungroup" else "Group by Base")
                    }
                }
            }

            item {
                val tiles = listOf(
                    Triple("Current", currentAssets.size, "Self: $currentSelfCount | Other: $currentOtherCount") to LocationAssetTab.CURRENT,
                    Triple("Belonging", belongingAssets.size, "At home: $belongingAtHome | Other: $belongingElsewhere") to LocationAssetTab.BELONGING,
                    Triple("Missing", missingCount, "Current empty") to LocationAssetTab.MISSING
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    tiles.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { (data, tab) ->
                                SummaryTile(
                                    title = data.first,
                                    count = data.second,
                                    subtitle = data.third,
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (selectedAssets.isEmpty()) {
                item {
                    EmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        message = "No assets in this view"
                    )
                }
            } else if (isGrouped) {
                item {
                    GroupedAssetList(
                        assets = selectedAssets,
                        locationId = locationId,
                        onDetach = onDetach,
                        onPrint = onPrintAsset,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(selectedAssets, key = { it.id }) { asset ->
                    AssetInLocationCard(
                        asset = asset,
                        onDetach = { onDetach(asset) },
                        onPrint = { onPrintAsset(asset) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

private enum class LocationAssetTab { CURRENT, BELONGING, MISSING }

@Composable
private fun SummaryTile(
    title: String,
    count: Int,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 6.dp else 2.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("$count", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChildLocationCard(
    location: LocationSummary,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (showBarcode, setShowBarcode) = remember { mutableStateOf(false) }
    val barcodeBitmap = rememberBarcodeImage(content = location.id.toString().padStart(6, '0'), width = 800, height = 220)
    Card(
        modifier = modifier
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

            // Barcode Image (hidden by default)
            if (showBarcode && barcodeBitmap != null) {
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
                        imageVector = Icons.Default.QrCode,
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
private fun AssetInLocationCard(
    asset: AssetSummary,
    onDetach: () -> Unit,
    onPrint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (showBarcode, setShowBarcode) = remember { mutableStateOf(false) }
    val barcodeBitmap = rememberBarcodeImage(content = asset.id.toString().padStart(6, '0'), width = 800, height = 220)
    Card(
        modifier = modifier.fillMaxWidth(),
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

            // Asset Status (match AssetsScreen styling)
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
                                "At Home" -> Color(0xFF10B981).copy(alpha = 0.1f)
                                "Deployed" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                                "Missing" -> Color(0xFFEF4444).copy(alpha = 0.1f)
                                "Not Assigned" -> Color(0xFF3B82F6).copy(alpha = 0.1f)
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
                                "At Home" -> Color(0xFF10B981)
                                "Deployed" -> Color(0xFFF59E0B)
                                "Missing" -> Color(0xFFEF4444)
                                "Not Assigned" -> Color(0xFF3B82F6)
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
                    text = buildString {
                        append("Base: ")
                        append(asset.baseRoomName ?: "Unassigned")
                        if (asset.currentRoomName != null && asset.currentRoomName != asset.baseRoomName) {
                            append(" | Current: ${asset.currentRoomName}")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Barcode (hidden by default)
            if (showBarcode && barcodeBitmap != null) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        bitmap = barcodeBitmap,
                        contentDescription = "Asset barcode",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
            }

            // Action Button
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { setShowBarcode(!showBarcode) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
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
private fun GroupedAssetList(
    assets: List<AssetSummary>,
    locationId: Long,
    onDetach: (AssetSummary) -> Unit,
    onPrint: (AssetSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = assets.groupBy { it.baseRoomId }
    val sortedKeys = grouped.keys.sortedWith(compareBy<Long?> { if (it == locationId) 0 else 1 }.thenBy { it ?: Long.MAX_VALUE })

    Column(
        modifier = modifier.fillMaxWidth(),
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
            Text(
                text = headerText,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            groupAssets.forEach { asset ->
                AssetInLocationCard(
                    asset = asset,
                    onDetach = { onDetach(asset) },
                    onPrint = { onPrint(asset) }
                )
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

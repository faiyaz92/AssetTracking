@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.assets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.presentation.tabs.model.AssetListEvent
import com.example.assettracking.presentation.tabs.viewmodel.AssetListViewModel
import com.example.assettracking.util.printBarcode
import com.example.assettracking.util.rememberBarcodeImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import com.example.assettracking.domain.model.RoomSummary

@Composable
fun AssetsScreen(
    onBack: () -> Unit,
    viewModel: AssetListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
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
            printBarcode(context, assetToPrint.code, assetToPrint.name)
            pendingPrintAsset = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAsset = null
                    showAssetDialog = true
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add asset")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { query ->
                    viewModel.onEvent(AssetListEvent.UpdateSearch(query))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = { Text("Search asset") }
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
                                viewModel.onEvent(AssetListEvent.DeleteAsset(asset.id))
                            },
                            onPrint = {
                                val allGranted = bluetoothPermissions.all { permission ->
                                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                                }
                                if (allGranted) {
                                    printBarcode(context, asset.code, asset.name)
                                } else {
                                    pendingPrintAsset = asset
                                    permissionLauncher.launch(bluetoothPermissions)
                                }
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
            initialCode = asset?.code.orEmpty(),
            initialName = asset?.name.orEmpty(),
            initialDetails = asset?.details.orEmpty(),
            initialBaseRoomId = asset?.baseRoomId,
            rooms = state.rooms,
            onDismiss = { showAssetDialog = false },
            onConfirm = { code, name, details, baseRoomId ->
                if (asset == null) {
                    viewModel.onEvent(AssetListEvent.CreateAsset(code, name, details, baseRoomId))
                } else {
                    viewModel.onEvent(AssetListEvent.UpdateAsset(asset.id, code, name, details))
                }
                showAssetDialog = false
            }
        )
    }
}

@Composable
private fun AssetCard(
    asset: AssetSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrint: () -> Unit
) {
    val barcodeBitmap = rememberBarcodeImage(content = asset.code, width = 800, height = 220)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = asset.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = "Code: ${asset.code}", style = MaterialTheme.typography.bodyMedium)
            asset.details?.takeIf { it.isNotBlank() }?.let { details ->
                Spacer(Modifier.height(4.dp))
                Text(text = details, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append("Base: ")
                    append(asset.baseRoomName ?: "Unassigned")
                    if (asset.currentRoomName != null && asset.currentRoomName != asset.baseRoomName) {
                        append(" | Current: ${asset.currentRoomName}")
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )
            barcodeBitmap?.let { bitmap ->
                Spacer(Modifier.height(12.dp))
                Image(
                    bitmap = bitmap,
                    contentDescription = "Barcode for ${asset.code}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrint) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = "Print barcode")
                }
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit asset")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete asset")
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
    initialCode: String,
    initialName: String,
    initialDetails: String,
    initialBaseRoomId: Long?,
    rooms: List<RoomSummary>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, Long?) -> Unit
) {
    val code = rememberSaveable(initialCode) { mutableStateOf(initialCode) }
    val name = rememberSaveable(initialName) { mutableStateOf(initialName) }
    val details = rememberSaveable(initialDetails) { mutableStateOf(initialDetails) }
    val baseRoomId = remember { mutableStateOf(initialBaseRoomId) }
    val expanded = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code.value,
                    onValueChange = { code.value = it },
                    label = { Text("Asset code") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = name.value,
                    onValueChange = { name.value = it },
                    label = { Text("Asset name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = details.value,
                    onValueChange = { details.value = it },
                    label = { Text("Details (optional)") }
                )
                ExposedDropdownMenuBox(
                    expanded = expanded.value,
                    onExpandedChange = { expanded.value = it }
                ) {
                    OutlinedTextField(
                        value = rooms.find { it.id == baseRoomId.value }?.name ?: "No base room",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Base room (optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No base room") },
                            onClick = {
                                baseRoomId.value = null
                                expanded.value = false
                            }
                        )
                        rooms.forEach { room ->
                            DropdownMenuItem(
                                text = { Text(room.name) },
                                onClick = {
                                    baseRoomId.value = room.id
                                    expanded.value = false
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
                        code.value,
                        name.value,
                        details.value.takeIf { it.isNotBlank() },
                        baseRoomId.value
                    )
                },
                enabled = code.value.isNotBlank() && name.value.isNotBlank()
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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.locationdetail

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assettracking.domain.model.AssetSummary
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlin.comparisons.compareBy

@Composable
fun LocationDetailScreen(
    viewModel: LocationDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message?.text
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    val (showDeleteDialog, setShowDeleteDialog) = remember { mutableStateOf(false) }
    val (assetToDelete, setAssetToDelete) = remember { mutableStateOf<AssetSummary?>(null) }

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
            FloatingActionButton(
                onClick = { scannerLauncher.launch(scanOptions) }
            ) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
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
                        assets = detail.assets,
                        isGrouped = state.isGrouped,
                        onToggleGrouping = { viewModel.toggleGrouping() },
                        locationId = detail.id,
                        onDetach = { asset ->
                            setAssetToDelete(asset)
                            setShowDeleteDialog(true)
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
}

@Composable
private fun LocationDetailContent(
    modifier: Modifier,
    locationName: String,
    locationDescription: String?,
    assets: List<AssetSummary>,
    isGrouped: Boolean,
    onToggleGrouping: () -> Unit,
    locationId: Long,
    onDetach: (AssetSummary) -> Unit
) {
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
                Text(
                    locationName,
                    style = MaterialTheme.typography.headlineMedium.copy(
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
                    "Manage assets in this location",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
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
        }
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
            item {
                Text(
                    text = headerText,
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

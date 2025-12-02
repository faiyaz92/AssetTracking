package com.example.assettracking.presentation.roomdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.util.rememberBarcodeImage
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    viewModel: RoomDetailViewModel = hiltViewModel(),
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

    val scannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
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
            setOrientationLocked(false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.roomDetail?.name ?: "Room") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
            state.roomDetail == null -> EmptyState(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                message = "Room not found"
            )
            else -> {
                val detail = state.roomDetail
                if (detail != null) {
                    RoomDetailContent(
                        modifier = Modifier.padding(paddingValues).fillMaxSize(),
                        roomName = detail.name,
                        roomDescription = detail.description,
                        assets = detail.assets,
                        onDetach = { assetId -> viewModel.detachAsset(assetId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomDetailContent(
    modifier: Modifier,
    roomName: String,
    roomDescription: String?,
    assets: List<AssetSummary>,
    onDetach: (Long) -> Unit
) {
    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = roomName, style = MaterialTheme.typography.headlineSmall)
            roomDescription?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Assets (${assets.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (assets.isEmpty()) {
            EmptyState(
                modifier = Modifier.fillMaxSize(),
                message = "No assets linked yet. Tap the scan button to add one."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(assets, key = { it.id }) { asset ->
                    AssetInRoomCard(asset = asset, onDetach = { onDetach(asset.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetInRoomCard(
    asset: AssetSummary,
    onDetach: () -> Unit
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
            barcodeBitmap?.let { bitmap ->
                Spacer(Modifier.height(12.dp))
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "Barcode for ${asset.code}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDetach) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Detach asset")
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

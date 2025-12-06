@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.tabs

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.assettracking.presentation.tabs.HomeViewModel
import com.example.assettracking.domain.model.LocationSummary
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

enum class ScanType {
    Barcode, RFID
}

data class DashboardItem(
    val title: String,
    val subtitle: String,
    val icon: @Composable () -> Unit,
    val gradient: Brush,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    onOpenLocations: () -> Unit,
    onOpenAssets: () -> Unit,
    onOpenAuditTrail: () -> Unit,
    onQuickScan: () -> Unit,
    rooms: List<LocationSummary> = emptyList(),
    onAssetMoved: (String, Long, String) -> Unit = { _, _, _ -> },
    viewModel: HomeViewModel = hiltViewModel()
) {
    val showQuickScanDialog = remember { mutableStateOf(false) }
    val rfidScanState by viewModel.rfidScanState.collectAsState()
    val dashboardItems = listOf(
        DashboardItem(
            title = "Locations",
            subtitle = "Manage Locations",
            icon = {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            },
            gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E40AF),
                    Color(0xFF3B82F6)
                )
            ),
            onClick = onOpenLocations
        ),
        DashboardItem(
            title = "Assets",
            subtitle = "Track Inventory",
            icon = {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            },
            gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF059669),
                    Color(0xFF10B981)
                )
            ),
            onClick = onOpenAssets
        ),
        DashboardItem(
            title = "Scan",
            subtitle = "Quick Access",
            icon = {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            },
            gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFDC2626),
                    Color(0xFFEF4444)
                )
            ),
            onClick = { showQuickScanDialog.value = true }
        ),
        DashboardItem(
            title = "Analytics",
            subtitle = "Reports & Insights",
            icon = {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            },
            gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF7C3AED),
                    Color(0xFF8B5CF6)
                )
            ),
            onClick = { /* TODO: Implement analytics */ }
        ),
        DashboardItem(
            title = "Audit Trail",
            subtitle = "Activity History",
            icon = {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            },
            gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF59E0B),
                    Color(0xFFFBBF24)
                )
            ),
            onClick = onOpenAuditTrail
        ),
        DashboardItem(
            title = "Settings",
            subtitle = "App Preferences",
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            },
            gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF6B7280),
                    Color(0xFF9CA3AF)
                )
            ),
            onClick = { /* TODO: Implement settings */ }
        )
    )

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
                            "AssetTrack Pro",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    label = { Text("Analytics") },
                    selected = false,
                    onClick = { }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            // Welcome Header
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
                        "Welcome Back!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Manage your assets efficiently with our professional tools",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    )
                }
            }

            // Dashboard Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(dashboardItems) { item ->
                    DashboardCard(item)
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(item: DashboardItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = item.onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(item.gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon at top
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    item.icon()
                }

                // Text at bottom
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun QuickScanDialog(
    rooms: List<LocationSummary>,
    onDismiss: () -> Unit,
    onScanComplete: (String, Long, String) -> Unit,
    viewModel: HomeViewModel
) {
    val scannedCode = remember { mutableStateOf<String?>(null) }
    val selectedRoomId = remember { mutableStateOf<Long?>(null) }
    val condition = remember { mutableStateOf("") }
    val showConditionDialog = remember { mutableStateOf(false) }
    val scanType = remember { mutableStateOf<ScanType>(ScanType.Barcode) }

    val rfidScanState by viewModel.rfidScanState.collectAsState()

    val context = LocalContext.current
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        val contents = result.contents
        if (contents != null) {
            scannedCode.value = contents
            showConditionDialog.value = true
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

    if (showConditionDialog.value && scannedCode.value != null) {
        AlertDialog(
            onDismissRequest = { 
                showConditionDialog.value = false
                scannedCode.value = null
                selectedRoomId.value = null
                condition.value = ""
            },
            title = { Text("Asset Scanned") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Asset Code: ${scannedCode.value}")

                    OutlinedTextField(
                        value = condition.value,
                        onValueChange = { condition.value = it },
                        label = { Text("Condition Description") },
                        placeholder = { Text("Describe the asset's condition") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Text(
                        "Select destination location:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    rooms.forEach { room ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedRoomId.value = room.id },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRoomId.value == room.id,
                                onClick = { selectedRoomId.value = room.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(room.name, style = MaterialTheme.typography.bodyMedium)
                                room.description?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val roomId = selectedRoomId.value
                        if (roomId != null) {
                            onScanComplete(scannedCode.value!!, roomId, condition.value)
                            showConditionDialog.value = false
                            scannedCode.value = null
                            selectedRoomId.value = null
                            condition.value = ""
                            onDismiss()
                        }
                    },
                    enabled = selectedRoomId.value != null
                ) {
                    Text("Move Asset")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showConditionDialog.value = false
                    scannedCode.value = null
                    selectedRoomId.value = null
                    condition.value = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Scan") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Scan an asset to quickly move it to a different location",
                    textAlign = TextAlign.Center
                )

                // Scan type selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { scanType.value = ScanType.Barcode },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (scanType.value == ScanType.Barcode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Barcode")
                    }
                    OutlinedButton(
                        onClick = { scanType.value = ScanType.RFID },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (scanType.value == ScanType.RFID) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.RssFeed, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RFID")
                    }
                }

                when (scanType.value) {
                    ScanType.Barcode -> {
                        Button(
                            onClick = {
                                scannerLauncher.launch(scanOptions)
                                // Don't dismiss - let condition dialog handle dismissal
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Barcode Scanning")
                        }
                    }
                    ScanType.RFID -> {
                        when (rfidScanState) {
                            is HomeViewModel.RfidScanState.Idle -> {
                                Button(
                                    onClick = { viewModel.startRfidScan() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.RssFeed, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start RFID Scanning")
                                }
                            }
                            is HomeViewModel.RfidScanState.Scanning -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scanning for RFID tags...")
                                }
                            }
                            is HomeViewModel.RfidScanState.Success -> {
                                val tags = (rfidScanState as HomeViewModel.RfidScanState.Success).tags
                                if (tags.isEmpty()) {
                                    Text("No RFID tags found. Try again.")
                                    Button(
                                        onClick = { viewModel.startRfidScan() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Scan Again")
                                    }
                                } else {
                                    Text("Select an asset tag:")
                                    tags.forEach { tag ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    scannedCode.value = tag
                                                    showConditionDialog.value = true
                                                    viewModel.clearRfidScan()
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = false,
                                                onClick = {
                                                    scannedCode.value = tag
                                                    showConditionDialog.value = true
                                                    viewModel.clearRfidScan()
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Tag: $tag")
                                        }
                                    }
                                }
                            }
                            is HomeViewModel.RfidScanState.Error -> {
                                val error = (rfidScanState as HomeViewModel.RfidScanState.Error).message
                                Text("Scan failed: $error")
                                Button(
                                    onClick = { viewModel.startRfidScan() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Try Again")
                                }
                            }
                        }
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
}

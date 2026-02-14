@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.rfiddemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assettracking.util.C72RfidReader
import com.example.assettracking.util.RfidHardwareException
import com.rscja.deviceapi.entity.UHFTAGInfo
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TagInfo(
    val epc: String,
    val count: Int,
    val rssi: String,
    val phase: String = ""
)

@Composable
fun RfidRadarScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rfidReader = remember { C72RfidReader(context) }
    
    var isScanning by remember { mutableStateOf(false) }
    var tagList by remember { mutableStateOf<List<TagInfo>>(emptyList()) }
    var totalScanned by remember { mutableStateOf(0) }
    var elapsedTime by remember { mutableStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    // Timer for elapsed time
    LaunchedEffect(isScanning) {
        if (isScanning) {
            val startTime = System.currentTimeMillis()
            while (isScanning) {
                delay(100)
                elapsedTime = (System.currentTimeMillis() - startTime) / 1000f
            }
        }
    }

    fun startScanning() {
        scope.launch {
            try {
                rfidReader.initialize()
                isScanning = true
                errorMessage = null
                
                withContext(Dispatchers.IO) {
                    val foundTags = mutableMapOf<String, TagInfo>()
                    var scanTotal = 0
                    
                    rfidReader.setInventoryCallback(object : IUHFInventoryCallback {
                        override fun callback(uhftagInfo: UHFTAGInfo?) {
                            uhftagInfo?.let { info ->
                                val epc = info.epc ?: return
                                scanTotal++
                                
                                val existing = foundTags[epc]
                                foundTags[epc] = TagInfo(
                                    epc = epc,
                                    count = (existing?.count ?: 0) + 1,
                                    rssi = info.rssi ?: "",
                                    phase = info.phase?.toString() ?: ""
                                )
                                
                                scope.launch(Dispatchers.Main) {
                                    tagList = foundTags.values.sortedByDescending { it.count }
                                    totalScanned = scanTotal
                                }
                            }
                        }
                    })
                    
                    // Start continuous scanning
                    rfidReader.startInventoryTag()
                    
                    // Auto-stop after 30 seconds
                    delay(30000)
                    if (isScanning) {
                        rfidReader.stopInventory()
                        isScanning = false
                    }
                }
            } catch (e: RfidHardwareException) {
                errorMessage = e.message
                snackbarHostState.showSnackbar(e.message ?: "RFID error")
                isScanning = false
            } catch (e: Exception) {
                errorMessage = e.message
                snackbarHostState.showSnackbar(e.message ?: "Unknown error")
                isScanning = false
            }
        }
    }

    fun stopScanning() {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    rfidReader.stopInventory()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            isScanning = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        rfidReader.stopInventory()
                        rfidReader.close()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6))
                        )
                    )
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "RFID Radar Scanner",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            tagList = emptyList()
                            totalScanned = 0
                            elapsedTime = 0f
                        }) {
                            Icon(Icons.Default.Clear, "Clear", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Unique Tags", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${tagList.size}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Reads", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "$totalScanned",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Time", style = MaterialTheme.typography.bodySmall)
                        Text(
                            String.format("%.1fs", elapsedTime),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                }
            }

            // Recent Tags Box
            if (tagList.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Recent Tags (Last 3)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        tagList.take(3).forEach { tag ->
                            Text(
                                "• ${tag.epc}  (×${tag.count})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Control Button
            Button(
                onClick = {
                    if (isScanning) {
                        stopScanning()
                    } else {
                        startScanning()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Radar,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isScanning) "Stop Scanning" else "Start Radar Scan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Tag List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tagList, key = { it.epc }) { tag ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "EPC: ${tag.epc}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (tag.rssi.isNotEmpty()) {
                                    Text(
                                        "RSSI: ${tag.rssi}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "×${tag.count}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

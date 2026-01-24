@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.rfiddemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ReadMore
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assettracking.util.C72RfidReader
import com.example.assettracking.util.RfidHardwareException
import com.rscja.deviceapi.RFIDWithUHFUART
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RfidReadScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rfidReader = remember { C72RfidReader(context) }
    
    var selectedBank by remember { mutableStateOf(1) } // 0=RESERVED, 1=EPC, 2=TID, 3=USER
    var startAddress by remember { mutableStateOf("2") }
    var readLength by remember { mutableStateOf("6") }
    var password by remember { mutableStateOf("00000000") }
    var readData by remember { mutableStateOf("") }
    var isReading by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    val bankNames = listOf("RESERVED", "EPC", "TID", "USER")

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        rfidReader.close()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    fun readTag() {
        scope.launch {
            isReading = true
            try {
                rfidReader.initialize()
                
                val result = withContext(Dispatchers.IO) {
                    rfidReader.readData(
                        password,
                        selectedBank,
                        startAddress.toIntOrNull() ?: 2,
                        readLength.toIntOrNull() ?: 6
                    )
                }
                
                if (result != null && result.isNotEmpty()) {
                    readData = result
                    snackbarHostState.showSnackbar("Read successful!")
                } else {
                    snackbarHostState.showSnackbar("No data read. Place tag closer.")
                }
            } catch (e: RfidHardwareException) {
                snackbarHostState.showSnackbar(e.message ?: "RFID error")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(e.message ?: "Read failed")
            } finally {
                isReading = false
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
                            colors = listOf(Color(0xFF059669), Color(0xFF10B981))
                        )
                    )
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "RFID Tag Reader",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bank Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Memory Bank",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bankNames.forEachIndexed { index, name ->
                            FilterChip(
                                selected = selectedBank == index,
                                onClick = {
                                    selectedBank = index
                                    when (index) {
                                        0 -> { // RESERVED
                                            startAddress = "0"
                                            readLength = "4"
                                        }
                                        1 -> { // EPC
                                            startAddress = "2"
                                            readLength = "6"
                                        }
                                        else -> { // TID, USER
                                            startAddress = "0"
                                            readLength = "6"
                                        }
                                    }
                                },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }

            // Parameters
            OutlinedTextField(
                value = startAddress,
                onValueChange = { startAddress = it },
                label = { Text("Start Address (word)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = readLength,
                onValueChange = { readLength = it },
                label = { Text("Length (words)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Access Password (Hex)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("00000000") }
            )

            // Read Button
            Button(
                onClick = { readTag() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isReading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isReading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.ReadMore, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Read Tag Data", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Read Data Display
            if (readData.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Read Data:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            Text(
                                readData,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Instructions:",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. Select the memory bank to read from\n" +
                        "2. Set start address (in words)\n" +
                        "3. Set read length (in words)\n" +
                        "4. Enter access password (if required)\n" +
                        "5. Place RFID tag near device\n" +
                        "6. Click 'Read Tag Data'",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

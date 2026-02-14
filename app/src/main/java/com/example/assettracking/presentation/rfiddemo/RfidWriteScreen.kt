@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.rfiddemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
fun RfidWriteScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rfidReader = remember { C72RfidReader(context) }
    
    var selectedBank by remember { mutableStateOf(1) } // 0=RESERVED, 1=EPC, 2=TID, 3=USER
    var startAddress by remember { mutableStateOf("2") }
    var writeData by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("00000000") }
    var isWriting by remember { mutableStateOf(false) }
    var writeSuccess by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    val bankNames = listOf("RESERVED", "EPC", "TID", "USER")
    
    // Auto-calculate length based on data
    val calculatedLength = writeData.length / 4

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

    fun writeTag() {
        // Validation
        if (writeData.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar("Write data cannot be empty")
            }
            return
        }
        
        if (writeData.length % 4 != 0) {
            scope.launch {
                snackbarHostState.showSnackbar("Data length must be multiple of 4 (hex)")
            }
            return
        }
        
        if (!writeData.matches(Regex("[0-9A-Fa-f]*"))) {
            scope.launch {
                snackbarHostState.showSnackbar("Data must be hexadecimal")
            }
            return
        }

        scope.launch {
            isWriting = true
            try {
                rfidReader.initialize()
                
                val result = withContext(Dispatchers.IO) {
                    rfidReader.writeData(
                        password,
                        selectedBank,
                        startAddress.toIntOrNull() ?: 2,
                        calculatedLength,
                        writeData
                    )
                }
                
                if (result == true) {
                    writeSuccess = "Tag written successfully!"
                    snackbarHostState.showSnackbar("Write successful!")
                } else {
                    writeSuccess = null
                    snackbarHostState.showSnackbar("Write failed. Ensure tag is present.")
                }
            } catch (e: RfidHardwareException) {
                writeSuccess = null
                snackbarHostState.showSnackbar(e.message ?: "RFID error")
            } catch (e: Exception) {
                writeSuccess = null
                snackbarHostState.showSnackbar(e.message ?: "Write failed")
            } finally {
                isWriting = false
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
                            colors = listOf(Color(0xFFDC2626), Color(0xFFEF4444))
                        )
                    )
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "RFID Tag Writer",
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
                                        0 -> startAddress = "0" // RESERVED
                                        1 -> startAddress = "2" // EPC
                                        else -> startAddress = "0" // TID, USER
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
                value = writeData,
                onValueChange = { 
                    writeData = it.uppercase()
                },
                label = { Text("Data to Write (Hex)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
                placeholder = { Text("E200689450800000001234") },
                supportingText = {
                    Text("Length: ${calculatedLength} words (${writeData.length} hex chars)")
                }
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Access Password (Hex)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("00000000") }
            )

            // Write Button
            Button(
                onClick = { writeTag() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isWriting && writeData.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isWriting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Write to Tag", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Success Message
            if (writeSuccess != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "✓",
                            fontSize = 24.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            writeSuccess!!,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        )
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
                        "1. Select the memory bank to write to\n" +
                        "2. Set start address (in words)\n" +
                        "3. Enter data in hexadecimal format\n" +
                        "   • Must be multiple of 4 characters\n" +
                        "   • Example: E200689450800000001234\n" +
                        "4. Enter access password (if required)\n" +
                        "5. Place RFID tag near device\n" +
                        "6. Click 'Write to Tag'\n\n" +
                        "⚠️ Warning: Writing will overwrite existing data!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Example Data
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Example Data:",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Asset ID 001234:\nE200689450800000001234",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

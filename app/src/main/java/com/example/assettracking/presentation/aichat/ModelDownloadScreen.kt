package com.example.assettracking.presentation.aichat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
    onBack: () -> Unit,
    onChatWithModel: (LocalModel) -> Unit,
    viewModel: ModelDownloadViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopBar(onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Download AI models for offline chat. Choose from various models with different capabilities and sizes.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            items(LocalModel.values()) { model ->
                val status = state.statuses[model]
                ModelCard(
                    model = model,
                    status = status,
                    onDownload = { viewModel.download(model) },
                    onChat = { onChatWithModel(model) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "Model Downloads",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        modifier = Modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF1E40AF), Color(0xFF06B6D4))
            )
        )
    )
}

@Composable
private fun ModelCard(
    model: LocalModel,
    status: ModelStatus?,
    onDownload: () -> Unit,
    onChat: () -> Unit
) {
    val modelManager = LocalModelManager(LocalContext.current)
    val modelInfo = remember(model) { modelManager.infoFor(model) }
    
    val sizeText = remember(modelInfo.sizeBytes) {
        when {
            modelInfo.sizeBytes >= 1_000_000_000 -> "%.1f GB".format(modelInfo.sizeBytes / 1_000_000_000.0)
            modelInfo.sizeBytes >= 1_000_000 -> "%.1f MB".format(modelInfo.sizeBytes / 1_000_000.0)
            else -> "%.1f KB".format(modelInfo.sizeBytes / 1_000.0)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        modelInfo.displayName, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${sizeText} • ${modelInfo.minMemoryGB}GB RAM required", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Feature badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (modelInfo.supportsImage) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "🖼️ Vision",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (modelInfo.supportsAudio) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "🎵 Audio",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            if (modelInfo.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    modelInfo.description, 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            val isDownloaded = status?.isDownloaded == true
            val isDownloading = status?.progress in 1..99
            val progress = status?.progress ?: 0

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    isDownloading -> {
                        Column {
                            LinearProgressIndicator(
                                progress = progress / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Downloading... $progress%", style = MaterialTheme.typography.bodySmall)
                                status?.downloadSpeed?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            val downloaded = status?.downloadedBytes ?: 0
                            val total = status?.totalBytes ?: 0
                            if (total > 0) {
                                val downloadedMB = downloaded / 1_000_000.0
                                val totalMB = total / 1_000_000.0
                                Text("%.1f MB / %.1f MB".format(downloadedMB, totalMB), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    isDownloaded -> {
                        Text("✅ Downloaded", color = MaterialTheme.colorScheme.primary)
                    }
                    status?.error != null -> {
                        val errorText = status.error ?: ""
                        Column {
                            Text("❌ Failed: $errorText", color = MaterialTheme.colorScheme.error)
                            Button(onClick = onDownload, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Retry Download")
                            }
                        }
                    }
                    else -> {
                        Button(onClick = onDownload) {
                            Text("Download")
                        }
                        // Chat button commented out until LiteRT-LM is properly configured
                        /*
                        if (isDownloaded) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = onChat) {
                                Text("Chat")
                            }
                        }
                        */
                    }
                }
            }

            status?.filePath?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text("📁 Path: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

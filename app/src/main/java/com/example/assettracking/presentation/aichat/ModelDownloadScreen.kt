package com.example.assettracking.presentation.aichat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
    onBack: () -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Download on-device models for offline AI.",
                style = MaterialTheme.typography.bodyMedium
            )

            LocalModel.values().forEach { model ->
                val status = state.statuses[model]
                ModelCard(
                    model = model,
                    status = status,
                    onDownload = { viewModel.download(model) }
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
    onDownload: () -> Unit
) {
    val (title, description) = remember(model) {
        when (model) {
            LocalModel.Gemma -> "Gemma" to "Balanced responses; larger size"
            LocalModel.TinyLlama -> "TinyLlama" to "Smaller; faster on-device"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

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
                        Text("Downloaded", color = MaterialTheme.colorScheme.primary)
                    }
                    status?.error != null -> {
                        val errorText = status.error ?: ""
                        Column {
                            Text("Failed: $errorText", color = MaterialTheme.colorScheme.error)
                            Button(onClick = onDownload, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Retry Download")
                            }
                        }
                    }
                    else -> {
                        Button(onClick = onDownload) {
                            Text("Download")
                        }
                    }
                }
            }

            status?.filePath?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Path: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

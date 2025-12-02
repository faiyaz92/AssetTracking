@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.assettracking.presentation.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onOpenRooms: () -> Unit,
    onOpenAssets: () -> Unit,
    onOpenAuditTrail: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Asset Tracking") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeTile(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                title = "Rooms",
                description = "Create rooms, review counts, and drill into room details.",
                onClick = onOpenRooms
            )
            HomeTile(
                icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                title = "Assets",
                description = "Register assets, auto-generate barcodes, and manage assignments.",
                onClick = onOpenAssets
            )
            HomeTile(
                icon = { Icon(Icons.Default.History, contentDescription = null) },
                title = "Audit Trail",
                description = "View historical movements and location changes of assets.",
                onClick = onOpenAuditTrail
            )
        }
    }
}

@Composable
private fun HomeTile(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

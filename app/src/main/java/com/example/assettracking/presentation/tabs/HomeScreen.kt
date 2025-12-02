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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DashboardItem(
    val title: String,
    val subtitle: String,
    val icon: @Composable () -> Unit,
    val gradient: Brush,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    onOpenRooms: () -> Unit,
    onOpenAssets: () -> Unit,
    onOpenAuditTrail: () -> Unit
) {
    val dashboardItems = listOf(
        DashboardItem(
            title = "Rooms",
            subtitle = "Manage Locations",
            icon = {
                Icon(
                    Icons.Default.Home,
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
            onClick = onOpenRooms
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
            onClick = { /* TODO: Implement quick scan */ }
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
            TopAppBar(
                title = {
                    Text(
                        "AssetTrack Pro",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            )
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

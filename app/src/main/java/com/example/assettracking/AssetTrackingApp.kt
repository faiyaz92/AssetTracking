package com.example.assettracking

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.assettracking.presentation.navigation.AssetTrackingNavHost
import com.example.assettracking.ui.theme.AssetTrackingTheme

@Composable
fun AssetTrackingApp() {
    AssetTrackingTheme {
        val navController = rememberNavController()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AssetTrackingNavHost(navController = navController)
        }
    }
}

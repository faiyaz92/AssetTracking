package com.example.assettracking.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.assettracking.presentation.navigation.AssetTrackingNavHost
import com.example.assettracking.ui.theme.AssetTrackingTheme

@Composable
fun AssetTrackingApp() {
    AssetTrackingTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            AssetTrackingNavHost(navController = navController)
        }
    }
}

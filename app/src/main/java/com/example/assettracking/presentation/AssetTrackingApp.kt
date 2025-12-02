package com.example.assettracking.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.assettracking.presentation.navigation.AssetTrackingNavHost
import com.example.assettracking.presentation.splash.SplashScreen
import com.example.assettracking.ui.theme.AssetTrackingTheme

@Composable
fun AssetTrackingApp() {
    AssetTrackingTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var showSplash by remember { mutableStateOf(true) }

            if (showSplash) {
                SplashScreen(onSplashComplete = { showSplash = false })
            } else {
                val navController = rememberNavController()
                AssetTrackingNavHost(navController = navController)
            }
        }
    }
}

package com.example.assettracking.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import com.example.assettracking.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Animate in
        scale.animateTo(1f, animationSpec = tween(800))
        alpha.animateTo(1f, animationSpec = tween(600))

        // Wait for 2 seconds
        delay(2000)

        // Animate out
        alpha.animateTo(0f, animationSpec = tween(400))

        // Navigate to main screen
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // App Icon
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Icon",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )

            // App Name
            Text(
                text = "AssetTrack Pro",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 24.dp)
            )

            // Tagline
            Text(
                text = "Professional Asset Management",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
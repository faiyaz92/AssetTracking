package com.example.assettracking.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.assettracking.presentation.assets.AssetsScreen
import com.example.assettracking.presentation.locationdetail.LocationDetailScreen
import com.example.assettracking.presentation.locationdetail.LocationDetailViewModel
import com.example.assettracking.presentation.locations.LocationsScreen
import com.example.assettracking.presentation.tabs.AuditTrailScreen
import com.example.assettracking.presentation.tabs.HomeScreen
import com.example.assettracking.presentation.tabs.viewmodel.HomeViewModel

object Destinations {
    const val Home = "home"
    const val Locations = "locations"
    const val Assets = "assets"
    const val LocationDetail = "location_detail"
    const val AuditTrail = "audit_trail"
}

object Routes {
    const val Home = Destinations.Home
    const val Locations = Destinations.Locations
    const val Assets = Destinations.Assets
    const val LocationDetail = "${Destinations.LocationDetail}/{locationId}"
    const val AuditTrail = Destinations.AuditTrail
}

@Composable
fun AssetTrackingNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home
    ) {
        composable(Routes.Home) {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                onOpenLocations = { navController.navigate(Routes.Locations) },
                onOpenAssets = { navController.navigate(Routes.Assets) },
                onOpenAuditTrail = { navController.navigate(Routes.AuditTrail) },
                onQuickScan = { /* Not used */ },
                rooms = uiState.rooms,
                onAssetMoved = { assetCode: String, roomId: Long, condition: String ->
                    viewModel.assignAssetToRoom(assetCode, roomId, condition)
                }
            )
        }
        composable(Routes.Locations) {
            LocationsScreen(
                onBack = { navController.popBackStack() },
                onOpenLocation = { locationId ->
                    navController.navigate("${Destinations.LocationDetail}/$locationId")
                }
            )
        }
        composable(Routes.Assets) {
            AssetsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.AuditTrail) {
            AuditTrailScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.LocationDetail,
            arguments = listOf(navArgument("locationId") { type = NavType.LongType })
        ) {
            val viewModel: LocationDetailViewModel = hiltViewModel()
            LocationDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

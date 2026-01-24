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
import com.example.assettracking.presentation.assetdetails.AssetDetailsScreen
import com.example.assettracking.presentation.assets.AssetsScreen
import com.example.assettracking.presentation.locationdetail.LocationDetailScreen
import com.example.assettracking.presentation.locationdetail.LocationDetailViewModel
import com.example.assettracking.presentation.locations.LocationsScreen
import com.example.assettracking.presentation.rfiddemo.RfidRadarScreen
import com.example.assettracking.presentation.rfiddemo.RfidReadScreen
import com.example.assettracking.presentation.rfiddemo.RfidWriteScreen
import com.example.assettracking.presentation.tabs.AuditTrailScreen
import com.example.assettracking.presentation.tabs.HomeScreen
import com.example.assettracking.presentation.tabs.viewmodel.HomeViewModel

object Destinations {
    const val Home = "home"
    const val Locations = "locations"
    const val Assets = "assets"
    const val LocationDetail = "location_detail"
    const val AssetDetails = "asset_details"
    const val AuditTrail = "audit_trail"
    const val RfidRadar = "rfid_radar"
    const val RfidRead = "rfid_read"
    const val RfidWrite = "rfid_write"
}

object Routes {
    const val Home = Destinations.Home
    const val Locations = Destinations.Locations
    const val Assets = Destinations.Assets
    const val LocationDetail = "${Destinations.LocationDetail}/{locationIdentifier}"
    const val AssetDetails = "${Destinations.AssetDetails}/{assetId}"
    const val AuditTrail = Destinations.AuditTrail
    const val RfidRadar = Destinations.RfidRadar
    const val RfidRead = Destinations.RfidRead
    const val RfidWrite = Destinations.RfidWrite
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
            val context = androidx.compose.ui.platform.LocalContext.current
            
            HomeScreen(
                onOpenLocations = { navController.navigate(Routes.Locations) },
                onOpenAssets = { navController.navigate(Routes.Assets) },
                onOpenAuditTrail = { navController.navigate(Routes.AuditTrail) },
                onQuickScan = { /* Not used */ },
                onOpenRfidRadar = { navController.navigate(Routes.RfidRadar) },
                onOpenRfidRead = { navController.navigate(Routes.RfidRead) },
                onOpenRfidWrite = { navController.navigate(Routes.RfidWrite) },
                onOpenDemoApp = {
                    // Launch demo app's MainActivity
                    val intent = android.content.Intent().apply {
                        setClassName(
                            "com.example.uhf",
                            "com.example.uhf.activity.UHFMainActivity"
                        )
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        // Demo app not installed
                        android.widget.Toast.makeText(
                            context,
                            "Demo app not installed. Please install rfid_demo_test.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onLocationScanned = { locationId ->
                    navController.navigate("${Routes.LocationDetail.replace("{locationIdentifier}", locationId.toString())}")
                },
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
                    navController.navigate("${Routes.LocationDetail.replace("{locationIdentifier}", locationId.toString())}")
                }
            )
        }
        composable(Routes.Assets) {
            AssetsScreen(
                onBack = { navController.popBackStack() },
                onAssetClick = { assetId ->
                    navController.navigate("${Destinations.AssetDetails}/$assetId")
                }
            )
        }
        composable(Routes.AuditTrail) {
            AuditTrailScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.LocationDetail,
            arguments = listOf(navArgument("locationIdentifier") { type = NavType.StringType })
        ) {
            val locationIdentifier = it.arguments?.getString("locationIdentifier") ?: ""
            val viewModel: LocationDetailViewModel = hiltViewModel()
            // Pass the identifier to viewModel so it can resolve it
            viewModel.setLocationIdentifier(locationIdentifier)
            LocationDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                navController = navController
            )
        }
        composable(
            route = Routes.AssetDetails,
            arguments = listOf(navArgument("assetId") { type = NavType.LongType })
        ) {
            AssetDetailsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.RfidRadar) {
            RfidRadarScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.RfidRead) {
            RfidReadScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.RfidWrite) {
            RfidWriteScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

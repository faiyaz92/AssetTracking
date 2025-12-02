package com.example.assettracking.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.assettracking.presentation.assets.AssetsScreen
import com.example.assettracking.presentation.roomdetail.RoomDetailScreen
import com.example.assettracking.presentation.roomdetail.RoomDetailViewModel
import com.example.assettracking.presentation.rooms.RoomsScreen
import com.example.assettracking.presentation.tabs.AuditTrailScreen
import com.example.assettracking.presentation.tabs.HomeScreen

object Destinations {
    const val Home = "home"
    const val Rooms = "rooms"
    const val Assets = "assets"
    const val RoomDetail = "room_detail"
    const val AuditTrail = "audit_trail"
}

object Routes {
    const val Home = Destinations.Home
    const val Rooms = Destinations.Rooms
    const val Assets = Destinations.Assets
    const val RoomDetail = "${Destinations.RoomDetail}/{roomId}"
    const val AuditTrail = Destinations.AuditTrail
}

@Composable
fun AssetTrackingNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home
    ) {
        composable(Routes.Home) {
            HomeScreen(
                onOpenRooms = { navController.navigate(Routes.Rooms) },
                onOpenAssets = { navController.navigate(Routes.Assets) },
                onOpenAuditTrail = { navController.navigate(Routes.AuditTrail) }
            )
        }
        composable(Routes.Rooms) {
            RoomsScreen(
                onBack = { navController.popBackStack() },
                onOpenRoom = { roomId ->
                    navController.navigate("${Destinations.RoomDetail}/$roomId")
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
            route = Routes.RoomDetail,
            arguments = listOf(navArgument("roomId") { type = NavType.LongType })
        ) {
            val viewModel: RoomDetailViewModel = hiltViewModel()
            RoomDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

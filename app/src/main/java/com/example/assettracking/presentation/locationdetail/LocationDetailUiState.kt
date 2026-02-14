package com.example.assettracking.presentation.locationdetail

import com.example.assettracking.domain.model.LocationDetail
import com.example.assettracking.presentation.common.UiMessage

data class LocationDetailUiState(
    val locationDetail: LocationDetail? = null,
    val isLoading: Boolean = true,
    val message: UiMessage? = null,
    val isGrouped: Boolean = false,
    val scannedRfidTags: List<String> = emptyList(),
    val isRfidScanning: Boolean = false,
    val scanCompleted: Boolean = false,
    val totalScanned: Int = 0,
    val isScanStoppable: Boolean = false // true for bulk scan (can be stopped manually)
)

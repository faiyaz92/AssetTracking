package com.example.assettracking.presentation.tabs.model

import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.domain.model.LocationSummary
import com.example.assettracking.presentation.common.UiMessage

data class AssetListUiState(
    val assets: List<AssetSummary> = emptyList(),
    val filteredAssets: List<AssetSummary> = emptyList(),
    val rooms: List<LocationSummary> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val message: UiMessage? = null,
    // Grouping and filtering
    val isGroupedByCurrentLocation: Boolean = false,
    val isGroupedByBaseLocation: Boolean = false,
    val currentLocationFilter: Long? = null,
    val baseLocationFilter: Long? = null,
    val groupedAssets: Map<String, List<AssetSummary>> = emptyMap()
)

package com.example.assettracking.presentation.tabs.model

import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.domain.model.LocationSummary
import com.example.assettracking.presentation.common.UiMessage

enum class GroupingMode {
    NONE, BY_BASE_LOCATION, BY_CURRENT_LOCATION
}

data class SubGroup(
    val location: LocationSummary?,
    val assets: List<AssetSummary>
)

data class GroupedItem(
    val location: LocationSummary?,
    val assets: List<AssetSummary>,
    val subGroups: List<SubGroup>
)

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
    val statusFilter: String? = null,
    val groupedAssets: Map<String, List<AssetSummary>> = emptyMap(),
    val groupingMode: GroupingMode = GroupingMode.NONE,
    val groupedData: List<GroupedItem> = emptyList()
)

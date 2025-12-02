package com.example.assettracking.presentation.tabs.model

import com.example.assettracking.domain.model.LocationSummary
import com.example.assettracking.presentation.common.UiMessage

data class LocationListUiState(
    val locations: List<LocationSummary> = emptyList(),
    val isLoading: Boolean = true,
    val message: UiMessage? = null
)

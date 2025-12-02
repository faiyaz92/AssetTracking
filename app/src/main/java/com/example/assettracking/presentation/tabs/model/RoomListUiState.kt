package com.example.assettracking.presentation.tabs.model

import com.example.assettracking.domain.model.RoomSummary
import com.example.assettracking.presentation.common.UiMessage

data class RoomListUiState(
    val rooms: List<RoomSummary> = emptyList(),
    val isLoading: Boolean = true,
    val message: UiMessage? = null
)

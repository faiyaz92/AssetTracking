package com.example.assettracking.presentation.roomdetail

import com.example.assettracking.domain.model.LocationDetail
import com.example.assettracking.presentation.common.UiMessage

data class RoomDetailUiState(
    val roomDetail: LocationDetail? = null,
    val isLoading: Boolean = true,
    val message: UiMessage? = null,
    val isGrouped: Boolean = false
)

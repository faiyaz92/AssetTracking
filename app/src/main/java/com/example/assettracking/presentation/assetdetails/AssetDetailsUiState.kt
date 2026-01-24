package com.example.assettracking.presentation.assetdetails

import com.example.assettracking.domain.model.AssetMovement
import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.presentation.common.UiMessage

data class AssetDetailsUiState(
    val asset: AssetSummary? = null,
    val movements: List<AssetMovement> = emptyList(),
    val isLoading: Boolean = true,
    val message: UiMessage? = null
)
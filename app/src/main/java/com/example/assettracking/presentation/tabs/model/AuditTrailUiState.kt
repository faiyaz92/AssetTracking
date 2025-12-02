package com.example.assettracking.presentation.tabs.model

import com.example.assettracking.domain.model.AssetMovement

data class AuditTrailUiState(
    val movements: List<AssetMovement> = emptyList(),
    val isLoading: Boolean = true
)
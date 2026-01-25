package com.example.assettracking.presentation.audit

import com.example.assettracking.domain.model.AuditRecord
import com.example.assettracking.domain.model.AssetMovement
import com.example.assettracking.domain.model.LocationSummary
import com.example.assettracking.presentation.common.UiMessage

data class AuditDetailUiState(
    val audit: AuditRecord? = null,
    val movements: List<AssetMovement> = emptyList(),
    val locations: List<LocationSummary> = emptyList(),
    val assetBases: Map<Long, Long?> = emptyMap(),
    val isLoading: Boolean = true,
    val message: UiMessage? = null
)

package com.example.assettracking.presentation.audit

import com.example.assettracking.domain.model.AuditRecord
import com.example.assettracking.domain.model.LocationSummary
import com.example.assettracking.presentation.common.UiMessage

data class AuditUiState(
    val audits: List<AuditRecord> = emptyList(),
    val locations: List<LocationSummary> = emptyList(),
    val isLoading: Boolean = true,
    val message: UiMessage? = null
)

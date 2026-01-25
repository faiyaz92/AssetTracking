package com.example.assettracking.domain.model

data class AuditCreateRequest(
    val name: String,
    val locationId: Long,
    val type: AuditType,
    val includeChildren: Boolean
)

package com.example.assettracking.domain.model

data class AuditRecord(
    val id: Long,
    val name: String,
    val type: AuditType,
    val includeChildren: Boolean,
    val locationId: Long,
    val locationName: String,
    val locationCode: String,
    val createdAt: Long,
    val finishedAt: Long?
) {
    val isFinished: Boolean get() = finishedAt != null
}

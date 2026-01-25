package com.example.assettracking.data.local.model

data class AuditWithLocationTuple(
    val auditId: Long,
    val name: String,
    val type: String,
    val includeChildren: Boolean,
    val locationId: Long,
    val locationName: String,
    val locationCode: String,
    val createdAt: Long,
    val finishedAt: Long?
)

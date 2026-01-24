package com.example.assettracking.domain.model

data class LocationSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val assetCount: Int,
    val parentId: Long?,
    val hasChildren: Boolean,
    val locationCode: String
)

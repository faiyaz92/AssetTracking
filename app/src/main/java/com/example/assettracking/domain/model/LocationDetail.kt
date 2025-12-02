package com.example.assettracking.domain.model

data class LocationDetail(
    val id: Long,
    val name: String,
    val description: String?,
    val assets: List<AssetSummary>
)

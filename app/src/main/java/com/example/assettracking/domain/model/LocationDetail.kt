package com.example.assettracking.domain.model

data class LocationDetail(
    val id: Long,
    val name: String,
    val description: String?,
    val locationCode: String,
    val children: List<LocationSummary>,
    val assets: List<AssetSummary>
)

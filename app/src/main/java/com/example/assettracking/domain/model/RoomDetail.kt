package com.example.assettracking.domain.model

data class RoomDetail(
    val id: Long,
    val name: String,
    val description: String?,
    val assets: List<AssetSummary>
)

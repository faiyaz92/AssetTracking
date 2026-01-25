package com.example.assettracking.domain.model

data class AssetSummary(
    val id: Long,
    val name: String,
    val details: String?,
    val condition: String?,
    val status: String,
    val baseRoomId: Long?,
    val baseRoomName: String?,
    val currentRoomId: Long?,
    val currentRoomName: String?
)

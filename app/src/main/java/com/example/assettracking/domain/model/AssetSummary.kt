package com.example.assettracking.domain.model

data class AssetSummary(
    val id: Long,
    val code: String,
    val name: String,
    val details: String?,
    val baseRoomId: Long?,
    val baseRoomName: String?,
    val currentRoomId: Long?,
    val currentRoomName: String?
)

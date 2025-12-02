package com.example.assettracking.domain.model

data class AssetMovement(
    val id: Long,
    val assetId: Long,
    val assetName: String,
    val fromRoomId: Long?,
    val fromRoomName: String?,
    val toRoomId: Long,
    val toRoomName: String,
    val condition: String?,
    val timestamp: Long
)
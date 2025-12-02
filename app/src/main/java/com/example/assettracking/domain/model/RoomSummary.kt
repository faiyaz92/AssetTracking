package com.example.assettracking.domain.model

data class RoomSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val assetCount: Int
)

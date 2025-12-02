package com.example.assettracking.domain.model

data class LocationSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val assetCount: Int
)

package com.example.assettracking.domain.repository

import com.example.assettracking.domain.model.AssetMovement
import kotlinx.coroutines.flow.Flow

interface AssetMovementRepository {
    fun observeMovements(): Flow<List<AssetMovement>>
    suspend fun createMovement(assetId: Long, fromRoomId: Long?, toRoomId: Long, condition: String? = null)
}
package com.example.assettracking.domain.repository

import com.example.assettracking.domain.model.AssetSummary
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    fun observeAssets(): Flow<List<AssetSummary>>
    suspend fun createAsset(name: String, details: String?, condition: String?, baseRoomId: Long?): Long
    suspend fun updateAsset(assetId: Long, name: String, details: String?, condition: String?, baseRoomId: Long?): Boolean
    suspend fun deleteAsset(assetId: Long): Boolean
    suspend fun assignAssetToRoom(assetId: Long, roomId: Long): Boolean
    suspend fun assignAssetToRoomWithCondition(assetId: Long, roomId: Long, condition: String?): Boolean
    suspend fun detachAssetFromRoom(assetId: Long): Boolean
    suspend fun findAssetById(assetId: Long): AssetSummary?
    suspend fun updateCurrentRoom(assetId: Long, currentRoomId: Long): Boolean
}

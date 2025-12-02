package com.example.assettracking.domain.repository

import com.example.assettracking.domain.model.AssetSummary
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    fun observeAssets(): Flow<List<AssetSummary>>
    suspend fun createAsset(code: String, name: String, details: String?, baseRoomId: Long?): Boolean
    suspend fun updateAsset(assetId: Long, code: String, name: String, details: String?): Boolean
    suspend fun deleteAsset(assetId: Long): Boolean
    suspend fun assignAssetToRoom(assetCode: String, roomId: Long): Boolean
    suspend fun detachAssetFromRoom(assetId: Long): Boolean
    suspend fun findAssetByCode(assetCode: String): AssetSummary?
    suspend fun updateCurrentRoom(assetId: Long, currentRoomId: Long): Boolean
}

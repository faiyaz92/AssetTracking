package com.example.assettracking.data.repository

import com.example.assettracking.data.local.dao.AssetDao
import com.example.assettracking.data.local.entity.AssetEntity
import com.example.assettracking.data.local.model.AssetWithRoomTuple
import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao
) : AssetRepository {

    override fun observeAssets(): Flow<List<AssetSummary>> =
        assetDao.observeAssets().map { items -> items.map { it.toDomainModel() } }

    override suspend fun createAsset(name: String, details: String?, condition: String?, baseRoomId: Long?): Long {
        val entity = AssetEntity(
            name = name.trim(),
            details = details?.trim(),
            condition = condition?.trim(),
            baseRoomId = baseRoomId,
            currentRoomId = baseRoomId // Initially, current room is the same as base room
        )
        return assetDao.insert(entity)
    }

    override suspend fun updateAsset(
        assetId: Long,
        name: String,
        details: String?,
        condition: String?,
        baseRoomId: Long?
    ): Boolean {
        val existing = assetDao.getAssetById(assetId) ?: return false
        val updated = existing.copy(
            name = name.trim(),
            details = details?.trim(),
            condition = condition?.trim(),
            baseRoomId = baseRoomId
        )
        assetDao.update(updated)
        return true
    }

    override suspend fun deleteAsset(assetId: Long): Boolean {
        val existing = assetDao.getAssetById(assetId) ?: return false
        assetDao.delete(existing)
        return true
    }

    override suspend fun assignAssetToRoom(assetId: Long, roomId: Long): Boolean {
        val asset = assetDao.getAssetById(assetId) ?: return false
        assetDao.attachAssetToRoom(asset.id, roomId)
        return true
    }

    override suspend fun assignAssetToRoomWithCondition(assetId: Long, roomId: Long, condition: String?): Boolean {
        val asset = assetDao.getAssetById(assetId) ?: return false
        // Update condition if provided
        if (condition != null) {
            val updatedAsset = asset.copy(condition = condition.trim())
            assetDao.update(updatedAsset)
        }
        assetDao.attachAssetToRoom(asset.id, roomId)
        return true
    }

    override suspend fun detachAssetFromRoom(assetId: Long): Boolean {
        val existing = assetDao.getAssetById(assetId) ?: return false
        assetDao.detachAssetFromRoom(existing.id)
        return true
    }

    override suspend fun findAssetById(assetId: Long): AssetSummary? {
        val asset = assetDao.getAssetById(assetId) ?: return null
        return asset.toDomainModel()
    }

    override suspend fun updateCurrentRoom(assetId: Long, currentRoomId: Long): Boolean {
        val existing = assetDao.getAssetById(assetId) ?: return false
        val updated = existing.copy(currentRoomId = currentRoomId)
        assetDao.update(updated)
        return true
    }

    private fun AssetWithRoomTuple.toDomainModel(): AssetSummary {
        val status = when {
            assetBaseRoomId == null -> "Not Assigned"
            assetCurrentRoomId == null -> "Missing"
            assetBaseRoomId != assetCurrentRoomId -> "Deployed"
            else -> "At Home"
        }
        return AssetSummary(
            id = assetId,
            name = assetName,
            details = assetDetails,
            condition = assetCondition,
            status = status,
            baseRoomId = assetBaseRoomId,
            baseRoomName = baseRoomName,
            currentRoomId = assetCurrentRoomId,
            currentRoomName = currentRoomName
        )
    }

    private fun AssetEntity.toDomainModel(): AssetSummary {
        val status = when {
            baseRoomId == null -> "Not Assigned"
            currentRoomId == null -> "Missing"
            baseRoomId != currentRoomId -> "Deployed"
            else -> "At Home"
        }
        return AssetSummary(
            id = id,
            name = name,
            details = details,
            condition = condition,
            status = status,
            baseRoomId = baseRoomId,
            baseRoomName = null,
            currentRoomId = currentRoomId,
            currentRoomName = null
        )
    }
}

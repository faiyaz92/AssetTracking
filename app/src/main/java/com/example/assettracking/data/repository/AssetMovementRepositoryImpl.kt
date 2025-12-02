package com.example.assettracking.data.repository

import com.example.assettracking.data.local.dao.AssetMovementDao
import com.example.assettracking.data.local.entity.AssetMovementEntity
import com.example.assettracking.domain.model.AssetMovement
import com.example.assettracking.domain.repository.AssetMovementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AssetMovementRepositoryImpl @Inject constructor(
    private val assetMovementDao: AssetMovementDao
) : AssetMovementRepository {

    override fun observeMovements(): Flow<List<AssetMovement>> {
        return assetMovementDao.observeMovements().map { tuples ->
            tuples.map { tuple ->
                AssetMovement(
                    id = tuple.movementId,
                    assetId = tuple.assetId,
                    assetName = tuple.assetName,
                    fromRoomId = tuple.fromRoomId,
                    fromRoomName = tuple.fromRoomName,
                    toRoomId = tuple.toRoomId,
                    toRoomName = tuple.toRoomName,
                    condition = tuple.condition,
                    timestamp = tuple.timestamp
                )
            }
        }
    }

    override suspend fun createMovement(assetId: Long, fromRoomId: Long?, toRoomId: Long, condition: String?) {
        val entity = AssetMovementEntity(
            assetId = assetId,
            fromRoomId = fromRoomId,
            toRoomId = toRoomId,
            condition = condition,
            timestamp = System.currentTimeMillis()
        )
        assetMovementDao.insert(entity)
    }
}
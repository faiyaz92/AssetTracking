package com.example.assettracking.data.repository

import com.example.assettracking.data.local.dao.RoomDao
import com.example.assettracking.data.local.entity.RoomEntity
import com.example.assettracking.data.local.entity.RoomWithAssetsEntity
import com.example.assettracking.data.local.model.RoomAssetTuple
import com.example.assettracking.data.local.model.RoomSummaryTuple
import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.domain.model.RoomDetail
import com.example.assettracking.domain.model.RoomSummary
import com.example.assettracking.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val roomDao: RoomDao
) : RoomRepository {

    override fun observeRoomSummaries(): Flow<List<RoomSummary>> =
        roomDao.observeRoomSummaries().map { rooms -> rooms.map { it.toDomainModel() } }

    override fun observeRoomDetail(roomId: Long): Flow<RoomDetail?> =
        roomDao.observeRoomWithAssets(roomId).combine(roomDao.observeRoomAssets(roomId)) { roomEntity, assetTuples ->
            roomEntity?.let {
                RoomDetail(
                    id = it.room.id,
                    name = it.room.name,
                    description = it.room.description,
                    assets = assetTuples.map { tuple ->
                        AssetSummary(
                            id = tuple.assetId,
                            code = tuple.assetCode,
                            name = tuple.assetName,
                            details = tuple.assetDetails,
                            baseRoomId = tuple.assetBaseRoomId,
                            baseRoomName = tuple.baseRoomName,
                            currentRoomId = tuple.assetCurrentRoomId,
                            currentRoomName = it.room.name
                        )
                    }
                )
            }
        }

    override suspend fun createRoom(name: String, description: String?): Long {
        val entity = RoomEntity(name = name, description = description)
        return roomDao.insert(entity)
    }

    override suspend fun updateRoom(roomId: Long, name: String, description: String?): Boolean {
        val existing = roomDao.getRoomById(roomId) ?: return false
        val updated = existing.copy(name = name, description = description)
        roomDao.update(updated)
        return true
    }

    override suspend fun deleteRoom(roomId: Long): Boolean {
        val existing = roomDao.getRoomById(roomId) ?: return false
        roomDao.delete(existing)
        return true
    }

    private fun RoomSummaryTuple.toDomainModel(): RoomSummary = RoomSummary(
        id = roomId,
        name = roomName,
        description = roomDescription,
        assetCount = assetCount
    )

    private fun RoomWithAssetsEntity.toDomainModel(): RoomDetail = RoomDetail(
        id = room.id,
        name = room.name,
        description = room.description,
        assets = assets.map { asset ->
            AssetSummary(
                id = asset.id,
                code = asset.code,
                name = asset.name,
                details = asset.details,
                baseRoomId = asset.baseRoomId,
                baseRoomName = null, // TODO: join to get base room name
                currentRoomId = room.id,
                currentRoomName = room.name
            )
        }
    )
}

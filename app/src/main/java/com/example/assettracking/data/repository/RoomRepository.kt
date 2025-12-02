package com.example.assettracking.data.repository

import com.example.assettracking.data.local.dao.RoomDao
import com.example.assettracking.data.local.entity.RoomEntity
import com.example.assettracking.data.local.entity.RoomWithAssetsEntity
import com.example.assettracking.data.local.model.RoomSummaryTuple
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val roomDao: RoomDao
) {
    fun observeRoomSummaries(): Flow<List<RoomSummaryTuple>> = roomDao.observeRoomSummaries()

    fun observeRoomDetails(roomId: Long): Flow<RoomWithAssetsEntity?> = roomDao.observeRoomWithAssets(roomId)

    suspend fun createRoom(name: String, description: String?): Long {
        val entity = RoomEntity(name = name.trim(), description = description?.trim())
        return roomDao.insert(entity)
    }

    suspend fun updateRoom(roomId: Long, name: String, description: String?) {
        val existing = roomDao.getRoomById(roomId) ?: return
        val updated = existing.copy(name = name.trim(), description = description?.trim())
        roomDao.update(updated)
    }

    suspend fun deleteRoom(roomId: Long) {
        val existing = roomDao.getRoomById(roomId) ?: return
        roomDao.delete(existing)
    }
}

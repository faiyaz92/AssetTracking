package com.example.assettracking.domain.repository

import com.example.assettracking.domain.model.RoomDetail
import com.example.assettracking.domain.model.RoomSummary
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    fun observeRoomSummaries(): Flow<List<RoomSummary>>
    fun observeRoomDetail(roomId: Long): Flow<RoomDetail?>
    suspend fun createRoom(name: String, description: String?): Long
    suspend fun updateRoom(roomId: Long, name: String, description: String?): Boolean
    suspend fun deleteRoom(roomId: Long): Boolean
}

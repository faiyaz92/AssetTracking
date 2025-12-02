package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.RoomRepository
import javax.inject.Inject

class UpdateRoomUseCase @Inject constructor(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(roomId: Long, name: String, description: String?): Result<Unit> {
        val normalized = name.trim()
        if (normalized.isBlank()) return Result.failure(IllegalArgumentException("Room name required"))
        val updated = roomRepository.updateRoom(roomId, normalized, description?.trim())
        if (!updated) return Result.failure(IllegalArgumentException("Room not found"))
        return Result.success(Unit)
    }
}

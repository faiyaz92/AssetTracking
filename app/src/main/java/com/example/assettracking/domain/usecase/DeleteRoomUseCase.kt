package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.RoomRepository
import javax.inject.Inject

class DeleteRoomUseCase @Inject constructor(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(roomId: Long): Result<Unit> {
        val deleted = roomRepository.deleteRoom(roomId)
        return if (deleted) Result.success(Unit) else Result.failure(IllegalArgumentException("Room not found"))
    }
}

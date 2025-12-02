package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.RoomRepository
import javax.inject.Inject

class CreateRoomUseCase @Inject constructor(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(name: String, description: String?): Result<Long> {
        val normalized = name.trim()
        if (normalized.isBlank()) return Result.failure(IllegalArgumentException("Room name required"))
        val id = roomRepository.createRoom(normalized, description?.trim())
        return Result.success(id)
    }
}

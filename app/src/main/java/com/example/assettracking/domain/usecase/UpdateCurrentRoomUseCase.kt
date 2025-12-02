package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.AssetRepository
import javax.inject.Inject

class UpdateCurrentRoomUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(assetId: Long, currentRoomId: Long): Result<Unit> {
        return if (assetRepository.updateCurrentRoom(assetId, currentRoomId)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Asset not found"))
        }
    }
}
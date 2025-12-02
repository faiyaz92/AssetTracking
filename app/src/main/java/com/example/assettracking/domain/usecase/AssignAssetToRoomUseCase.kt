package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.AssetRepository
import javax.inject.Inject

class AssignAssetToRoomUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(assetCode: String, roomId: Long): Result<Unit> {
        return if (assetRepository.assignAssetToRoom(assetCode, roomId)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Asset not found"))
        }
    }
}

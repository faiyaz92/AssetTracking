package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.AssetRepository
import javax.inject.Inject

class AssignAssetToRoomWithConditionUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(assetCode: String, roomId: Long, condition: String?): Result<Unit> {
        return if (assetRepository.assignAssetToRoomWithCondition(assetCode, roomId, condition)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Asset not found"))
        }
    }
}
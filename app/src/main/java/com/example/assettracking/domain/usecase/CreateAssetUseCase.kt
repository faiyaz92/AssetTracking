package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.AssetRepository
import javax.inject.Inject

class CreateAssetUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(name: String, details: String?, condition: String?, baseRoomId: Long?): Result<Long> {
        val assetId = assetRepository.createAsset(name, details, condition, baseRoomId)
        return if (assetId > 0) {
            Result.success(assetId)
        } else {
            Result.failure(IllegalArgumentException("Failed to create asset"))
        }
    }
}

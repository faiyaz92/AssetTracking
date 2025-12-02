package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.AssetRepository
import javax.inject.Inject

class CreateAssetUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(code: String, name: String, details: String?, condition: String?, baseRoomId: Long?): Result<Unit> {
        return if (assetRepository.createAsset(code, name, details, condition, baseRoomId)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Asset code already exists or invalid"))
        }
    }
}

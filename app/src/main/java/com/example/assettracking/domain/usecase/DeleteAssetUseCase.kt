package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.AssetRepository
import javax.inject.Inject

class DeleteAssetUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(assetId: Long): Result<Unit> {
        return if (assetRepository.deleteAsset(assetId)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Asset not found"))
        }
    }
}

package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.AssetRepository
import javax.inject.Inject

class UpdateAssetUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(
        assetId: Long,
        code: String,
        name: String,
        details: String?,
        condition: String?
    ): Result<Unit> {
        return if (assetRepository.updateAsset(assetId, code, name, details, condition)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Unable to update asset"))
        }
    }
}

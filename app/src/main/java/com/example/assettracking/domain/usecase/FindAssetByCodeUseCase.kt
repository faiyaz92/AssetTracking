package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.domain.repository.AssetRepository
import javax.inject.Inject

class FindAssetByCodeUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(assetCode: String): AssetSummary? =
        assetRepository.findAssetByCode(assetCode)
}

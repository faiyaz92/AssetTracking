package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAssetsUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    operator fun invoke(): Flow<List<AssetSummary>> = assetRepository.observeAssets()
}

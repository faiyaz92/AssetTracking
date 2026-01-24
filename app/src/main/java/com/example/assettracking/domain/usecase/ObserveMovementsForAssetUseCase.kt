package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.model.AssetMovement
import com.example.assettracking.domain.repository.AssetMovementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMovementsForAssetUseCase @Inject constructor(
    private val assetMovementRepository: AssetMovementRepository
) {
    operator fun invoke(assetId: Long): Flow<List<AssetMovement>> {
        return assetMovementRepository.observeMovementsForAsset(assetId)
    }
}
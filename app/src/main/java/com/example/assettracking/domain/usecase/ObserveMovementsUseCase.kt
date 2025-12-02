package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.model.AssetMovement
import com.example.assettracking.domain.repository.AssetMovementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMovementsUseCase @Inject constructor(
    private val assetMovementRepository: AssetMovementRepository
) {
    operator fun invoke(): Flow<List<AssetMovement>> {
        return assetMovementRepository.observeMovements()
    }
}
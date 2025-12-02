package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.AssetMovementRepository
import javax.inject.Inject

class CreateMovementUseCase @Inject constructor(
    private val assetMovementRepository: AssetMovementRepository
) {
    suspend operator fun invoke(assetId: Long, fromRoomId: Long?, toRoomId: Long) {
        assetMovementRepository.createMovement(assetId, fromRoomId, toRoomId)
    }
}
package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.LocationRepository
import javax.inject.Inject

class UpdateRoomUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(locationId: Long, name: String, description: String?): Result<Unit> {
        val normalized = name.trim()
        if (normalized.isBlank()) return Result.failure(IllegalArgumentException("Location name required"))
        val updated = locationRepository.updateLocation(locationId, normalized, description?.trim())
        if (!updated) return Result.failure(IllegalArgumentException("Location not found"))
        return Result.success(Unit)
    }
}

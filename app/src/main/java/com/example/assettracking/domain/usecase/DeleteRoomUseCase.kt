package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.LocationRepository
import javax.inject.Inject

class DeleteRoomUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(locationId: Long): Result<Unit> {
        val deleted = locationRepository.deleteLocation(locationId)
        return if (deleted) Result.success(Unit) else Result.failure(IllegalArgumentException("Location not found"))
    }
}

package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.repository.LocationRepository
import javax.inject.Inject

class CreateRoomUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(name: String, description: String?, parentId: Long? = null): Result<Long> {
        val normalized = name.trim()
        if (normalized.isBlank()) return Result.failure(IllegalArgumentException("Location name required"))
        val id = locationRepository.createLocation(normalized, description?.trim(), parentId)
        return Result.success(id)
    }
}

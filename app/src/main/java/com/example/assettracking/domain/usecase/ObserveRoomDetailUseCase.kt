package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.model.LocationDetail
import com.example.assettracking.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRoomDetailUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    operator fun invoke(locationId: Long): Flow<LocationDetail?> = locationRepository.observeLocationDetail(locationId)
}

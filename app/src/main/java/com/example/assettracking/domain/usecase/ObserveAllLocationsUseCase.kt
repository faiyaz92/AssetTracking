package com.example.assettracking.domain.usecase

import com.example.assettracking.domain.model.LocationSummary
import com.example.assettracking.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAllLocationsUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    operator fun invoke(): Flow<List<LocationSummary>> = locationRepository.observeAllLocationSummaries()
}
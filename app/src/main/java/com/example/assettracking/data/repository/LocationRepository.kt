package com.example.assettracking.data.repository

import com.example.assettracking.domain.model.LocationDetail
import com.example.assettracking.domain.model.LocationSummary
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocationSummaries(): Flow<List<LocationSummary>>

    fun observeLocationDetail(locationId: Long): Flow<LocationDetail?>

    suspend fun createLocation(name: String, description: String?): Long

    suspend fun updateLocation(locationId: Long, name: String, description: String?): Boolean

    suspend fun deleteLocation(locationId: Long): Boolean
}

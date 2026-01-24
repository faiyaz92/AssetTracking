package com.example.assettracking.domain.repository

import com.example.assettracking.domain.model.LocationDetail
import com.example.assettracking.domain.model.LocationSummary
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocationSummaries(): Flow<List<LocationSummary>>
    fun observeAllLocationSummaries(): Flow<List<LocationSummary>>
    fun observeLocationDetail(locationId: Long): Flow<LocationDetail?>
    suspend fun createLocation(name: String, description: String?, parentId: Long? = null): Long
    suspend fun updateLocation(locationId: Long, name: String, description: String?): Boolean
    suspend fun deleteLocation(locationId: Long): Boolean
    suspend fun getLocationById(id: Long): LocationSummary?
    suspend fun getLocationByCode(code: String): LocationSummary?
}

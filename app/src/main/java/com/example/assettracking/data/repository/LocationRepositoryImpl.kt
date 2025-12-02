package com.example.assettracking.data.repository

import com.example.assettracking.data.local.dao.LocationDao
import com.example.assettracking.data.local.entity.LocationEntity
import com.example.assettracking.data.local.entity.RoomWithAssetsEntity
import com.example.assettracking.data.local.model.RoomAssetTuple
import com.example.assettracking.data.local.model.LocationSummaryTuple
import com.example.assettracking.domain.model.AssetSummary
import com.example.assettracking.domain.model.LocationDetail
import com.example.assettracking.domain.model.LocationSummary
import com.example.assettracking.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao
) : LocationRepository {

    override fun observeLocationSummaries(): Flow<List<LocationSummary>> =
        locationDao.observeLocationSummaries().map { locations -> locations.map { it.toDomainModel() } }

    override fun observeLocationDetail(locationId: Long): Flow<LocationDetail?> =
        locationDao.observeLocationWithAssets(locationId).combine(locationDao.observeLocationAssets(locationId)) { locationEntity, assetTuples ->
            locationEntity?.let {
                LocationDetail(
                    id = it.room.id,
                    name = it.room.name,
                    description = it.room.description,
                    assets = assetTuples.map { tuple ->
                        AssetSummary(
                            id = tuple.assetId,
                            name = tuple.assetName,
                            details = tuple.assetDetails,
                            condition = tuple.assetCondition,
                            baseRoomId = tuple.assetBaseRoomId,
                            baseRoomName = tuple.baseRoomName,
                            currentRoomId = tuple.assetCurrentRoomId,
                            currentRoomName = it.room.name
                        )
                    }
                )
            }
        }

    override suspend fun createLocation(name: String, description: String?): Long {
        val entity = LocationEntity(name = name, description = description)
        return locationDao.insert(entity)
    }

    override suspend fun updateLocation(locationId: Long, name: String, description: String?): Boolean {
        val existing = locationDao.getLocationById(locationId) ?: return false
        val updated = existing.copy(name = name, description = description)
        locationDao.update(updated)
        return true
    }

    override suspend fun deleteLocation(locationId: Long): Boolean {
        val existing = locationDao.getLocationById(locationId) ?: return false
        locationDao.delete(existing)
        return true
    }

    private fun LocationSummaryTuple.toDomainModel(): LocationSummary = LocationSummary(
        id = roomId,
        name = roomName,
        description = roomDescription,
        assetCount = assetCount
    )

    private fun RoomWithAssetsEntity.toDomainModel(): LocationDetail = LocationDetail(
        id = room.id,
        name = room.name,
        description = room.description,
        assets = assets.map { asset ->
            AssetSummary(
                id = asset.id,
                name = asset.name,
                details = asset.details,
                condition = asset.condition,
                baseRoomId = asset.baseRoomId,
                baseRoomName = null, // TODO: join to get base room name
                currentRoomId = room.id,
                currentRoomName = room.name
            )
        }
    )
}

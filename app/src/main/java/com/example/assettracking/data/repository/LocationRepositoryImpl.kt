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
import com.example.assettracking.util.LocationCodeGenerator
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

    override fun observeAllLocationSummaries(): Flow<List<LocationSummary>> =
        locationDao.observeAllLocationSummaries().map { locations -> locations.map { it.toDomainModel() } }

    override fun observeLocationDetail(locationId: Long): Flow<LocationDetail?> =
        locationDao.observeLocationWithAssets(locationId)
            .combine(locationDao.observeLocationAssets(locationId)) { locationEntity, assetTuples ->
                locationEntity?.let {
                    Pair(it, assetTuples)
                }
            }
            .combine(locationDao.observeChildLocations(locationId)) { pair, childrenTuples ->
                pair?.let { (locationEntity, assetTuples) ->
                    LocationDetail(
                        id = locationEntity.room.id,
                        name = locationEntity.room.name,
                        description = locationEntity.room.description,
                        locationCode = locationEntity.room.locationCode,
                        children = childrenTuples.map { it.toDomainModel() },
                        assets = assetTuples.map { tuple ->
                            val status = when {
                                tuple.assetBaseRoomId == null -> "Not Assigned"
                                tuple.assetCurrentRoomId == null -> "Missing"
                                tuple.assetBaseRoomId != tuple.assetCurrentRoomId -> "At Other Location"
                                else -> "At Home"
                            }
                            AssetSummary(
                                id = tuple.assetId,
                                name = tuple.assetName,
                                details = tuple.assetDetails,
                                condition = tuple.assetCondition,
                                baseRoomId = tuple.assetBaseRoomId,
                                baseRoomName = tuple.baseRoomName,
                                currentRoomId = tuple.assetCurrentRoomId,
                                currentRoomName = tuple.currentRoomName,
                                status = status
                            )
                        }
                    )
                }
            }

    override suspend fun createLocation(name: String, description: String?, parentId: Long?): Long {
        val locationCode = generateLocationCode(parentId)
        val entity = LocationEntity(
            name = name,
            description = description,
            parentId = parentId,
            locationCode = locationCode
        )
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
        assetCount = assetCount,
        parentId = parentId,
        hasChildren = hasChildren,
        locationCode = locationCode
    )

    private fun RoomWithAssetsEntity.toDomainModel(): LocationDetail = LocationDetail(
        id = room.id,
        name = room.name,
        description = room.description,
        locationCode = room.locationCode,
        children = emptyList(), // TODO: Get children
        assets = assets.map { asset ->
            val status = when {
                asset.baseRoomId == null -> "Not Assigned"
                asset.currentRoomId == null -> "Missing"
                asset.baseRoomId != asset.currentRoomId -> "At Other Location"
                else -> "At Home"
            }
            AssetSummary(
                id = asset.id,
                name = asset.name,
                details = asset.details,
                condition = asset.condition,
                baseRoomId = asset.baseRoomId,
                baseRoomName = null, // TODO: join to get base room name
                currentRoomId = room.id,
                currentRoomName = room.name,
                status = status
            )
        }
    )

    private suspend fun generateLocationCode(parentId: Long?): String {
        return if (parentId == null) {
            // Generate super parent code
            val existingCodes = locationDao.getAllSuperParentLocationCodes()
            LocationCodeGenerator.generateNextSuperParentCode(existingCodes)
        } else {
            // Generate child code
            val parentEntity = locationDao.getLocationById(parentId)
            val parentCode = parentEntity?.locationCode ?: "LOC-A" // fallback
            val existingChildCodes = locationDao.getChildLocationCodes(parentId)
            LocationCodeGenerator.generateNextChildCode(parentCode, existingChildCodes)
        }
    }

    override suspend fun getLocationById(id: Long): LocationSummary? {
        return locationDao.getLocationSummaryById(id)?.toDomainModel()
    }

    override suspend fun getLocationByCode(code: String): LocationSummary? {
        // First get the entity to check if it exists, then get the summary
        val entity = locationDao.getLocationByCode(code)
        return entity?.let { locationDao.getLocationSummaryById(it.id)?.toDomainModel() }
    }
}

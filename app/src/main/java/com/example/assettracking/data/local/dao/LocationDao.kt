package com.example.assettracking.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.assettracking.data.local.entity.LocationEntity
import com.example.assettracking.data.local.entity.RoomWithAssetsEntity
import com.example.assettracking.data.local.model.LocationSummaryTuple
import com.example.assettracking.data.local.model.RoomAssetTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insert(location: LocationEntity): Long

    @Update
    suspend fun update(location: LocationEntity)

    @Delete
    suspend fun delete(location: LocationEntity)

    @Query(
        """
        SELECT r.id AS roomId,
               r.name AS roomName,
               r.description AS roomDescription,
               COUNT(a.id) AS assetCount,
               r.parentId AS parentId,
               CASE WHEN EXISTS(SELECT 1 FROM locations c WHERE c.parentId = r.id) THEN 1 ELSE 0 END AS hasChildren,
               r.locationCode AS locationCode
        FROM locations r
        LEFT JOIN assets a ON a.currentRoomId = r.id
        WHERE r.parentId IS NULL
        GROUP BY r.id
        ORDER BY r.name
        """
    )
    fun observeLocationSummaries(): Flow<List<LocationSummaryTuple>>

    @Transaction
    @Query("SELECT * FROM locations WHERE id = :locationId")
    fun observeLocationWithAssets(locationId: Long): Flow<RoomWithAssetsEntity?>

    @Query(
        """
        SELECT a.id AS assetId,
               a.id AS assetCode,
               a.name AS assetName,
               a.details AS assetDetails,
               a.condition AS assetCondition,
               a.baseRoomId AS assetBaseRoomId,
               br.name AS baseRoomName,
               a.currentRoomId AS assetCurrentRoomId,
               cr.name AS currentRoomName
        FROM assets a
        LEFT JOIN locations br ON br.id = a.baseRoomId
        LEFT JOIN locations cr ON cr.id = a.currentRoomId
        WHERE a.currentRoomId = :locationId OR a.baseRoomId = :locationId
        ORDER BY a.name
        """
    )
    fun observeLocationAssets(locationId: Long): Flow<List<RoomAssetTuple>>

    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: Long): LocationEntity?

    @Query("SELECT * FROM locations WHERE locationCode = :code")
    suspend fun getLocationByCode(code: String): LocationEntity?

    @Query(
        """
        SELECT r.id AS roomId,
               r.name AS roomName,
               r.description AS roomDescription,
               COUNT(a.id) AS assetCount,
               r.parentId AS parentId,
               CASE WHEN EXISTS(SELECT 1 FROM locations c WHERE c.parentId = r.id) THEN 1 ELSE 0 END AS hasChildren,
               r.locationCode AS locationCode
        FROM locations r
        LEFT JOIN assets a ON a.currentRoomId = r.id
        WHERE r.id = :locationId
        GROUP BY r.id
        """
    )
    suspend fun getLocationSummaryById(locationId: Long): LocationSummaryTuple?

    @Query(
        """
        SELECT r.id AS roomId,
               r.name AS roomName,
               r.description AS roomDescription,
               COUNT(a.id) AS assetCount,
               r.parentId AS parentId,
               0 AS hasChildren,
               r.locationCode AS locationCode
        FROM locations r
        LEFT JOIN assets a ON a.currentRoomId = r.id
        GROUP BY r.id
        ORDER BY r.name
        """
    )
    fun observeAllLocationSummaries(): Flow<List<LocationSummaryTuple>>

    @Query("SELECT locationCode FROM locations WHERE parentId = :parentId ORDER BY locationCode")
    suspend fun getChildLocationCodes(parentId: Long): List<String>

    @Query("SELECT locationCode FROM locations WHERE parentId IS NULL ORDER BY locationCode")
    suspend fun getAllSuperParentLocationCodes(): List<String>

    @Query("SELECT id AS locationId, parentId FROM locations")
    suspend fun getLocationHierarchy(): List<com.example.assettracking.data.local.model.LocationParentTuple>

    @Query(
        """
        SELECT r.id AS roomId,
               r.name AS roomName,
               r.description AS roomDescription,
               COUNT(a.id) AS assetCount,
               r.parentId AS parentId,
               CASE WHEN EXISTS(SELECT 1 FROM locations c WHERE c.parentId = r.id) THEN 1 ELSE 0 END AS hasChildren,
               r.locationCode AS locationCode
        FROM locations r
        LEFT JOIN assets a ON a.currentRoomId = r.id
        WHERE r.parentId = :parentId
        GROUP BY r.id
        ORDER BY r.name
        """
    )
    fun observeChildLocations(parentId: Long): Flow<List<LocationSummaryTuple>>
}

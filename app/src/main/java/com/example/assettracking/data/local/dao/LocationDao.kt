package com.example.assettracking.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.assettracking.data.local.entity.LocationEntity
import com.example.assettracking.data.local.entity.RoomWithAssetsEntity
import com.example.assettracking.data.local.model.RoomAssetTuple
import com.example.assettracking.data.local.model.LocationSummaryTuple
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
        "SELECT r.id AS roomId, r.name AS roomName, r.description AS roomDescription, " +
            "COUNT(a.id) AS assetCount FROM locations r " +
            "LEFT JOIN assets a ON a.currentRoomId = r.id " +
            "GROUP BY r.id ORDER BY r.name"
    )
    fun observeLocationSummaries(): Flow<List<LocationSummaryTuple>>

    @Transaction
    @Query("SELECT * FROM locations WHERE id = :locationId")
    fun observeLocationWithAssets(locationId: Long): Flow<RoomWithAssetsEntity?>

    @Query(
        "SELECT a.id AS assetId, a.id AS assetCode, a.name AS assetName, a.details AS assetDetails, " +
            "a.condition AS assetCondition, a.baseRoomId AS assetBaseRoomId, br.name AS baseRoomName, a.currentRoomId AS assetCurrentRoomId " +
            "FROM assets a LEFT JOIN locations br ON br.id = a.baseRoomId " +
            "WHERE a.currentRoomId = :locationId ORDER BY a.name"
    )
    fun observeLocationAssets(locationId: Long): Flow<List<RoomAssetTuple>>

    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: Long): LocationEntity?
}

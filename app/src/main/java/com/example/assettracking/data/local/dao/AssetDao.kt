package com.example.assettracking.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.assettracking.data.local.entity.AssetEntity
import com.example.assettracking.data.local.model.AssetWithRoomTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Insert
    suspend fun insert(asset: AssetEntity): Long

    @Update
    suspend fun update(asset: AssetEntity)

    @Delete
    suspend fun delete(asset: AssetEntity)

    @Query(
        "SELECT a.id AS assetId, a.id AS assetCode, a.name AS assetName, a.details AS assetDetails, a.condition AS assetCondition, " +
            "a.baseRoomId AS assetBaseRoomId, br.name AS baseRoomName, " +
            "a.currentRoomId AS assetCurrentRoomId, cr.name AS currentRoomName " +
            "FROM assets a " +
            "LEFT JOIN locations br ON br.id = a.baseRoomId " +
            "LEFT JOIN locations cr ON cr.id = a.currentRoomId " +
            "ORDER BY a.name"
    )
    fun observeAssets(): Flow<List<AssetWithRoomTuple>>

    @Query("SELECT * FROM assets WHERE id = :assetId LIMIT 1")
    suspend fun getAssetById(assetId: Long): AssetEntity?

    @Query("UPDATE assets SET currentRoomId = :roomId WHERE id = :assetId")
    suspend fun attachAssetToRoom(assetId: Long, roomId: Long)

    @Query("UPDATE assets SET currentRoomId = NULL WHERE id = :assetId")
    suspend fun detachAssetFromRoom(assetId: Long)

    @Query("SELECT id FROM assets WHERE currentRoomId IN (:locationIds)")
    suspend fun getAssetIdsByCurrentLocations(locationIds: List<Long>): List<Long>

    @Query("SELECT id FROM assets WHERE baseRoomId IN (:locationIds)")
    suspend fun getAssetIdsByBaseLocations(locationIds: List<Long>): List<Long>

    @Query("UPDATE assets SET currentRoomId = NULL WHERE id IN (:assetIds)")
    suspend fun clearCurrentLocation(assetIds: List<Long>)
}

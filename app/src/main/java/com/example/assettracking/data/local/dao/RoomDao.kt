package com.example.assettracking.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.assettracking.data.local.entity.RoomEntity
import com.example.assettracking.data.local.entity.RoomWithAssetsEntity
import com.example.assettracking.data.local.model.RoomAssetTuple
import com.example.assettracking.data.local.model.RoomSummaryTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Insert
    suspend fun insert(room: RoomEntity): Long

    @Update
    suspend fun update(room: RoomEntity)

    @Delete
    suspend fun delete(room: RoomEntity)

    @Query(
        "SELECT r.id AS roomId, r.name AS roomName, r.description AS roomDescription, " +
            "COUNT(a.id) AS assetCount FROM rooms r " +
            "LEFT JOIN assets a ON a.currentRoomId = r.id " +
            "GROUP BY r.id ORDER BY r.name"
    )
    fun observeRoomSummaries(): Flow<List<RoomSummaryTuple>>

    @Transaction
    @Query("SELECT * FROM rooms WHERE id = :roomId")
    fun observeRoomWithAssets(roomId: Long): Flow<RoomWithAssetsEntity?>

    @Query(
        "SELECT a.id AS assetId, a.code AS assetCode, a.name AS assetName, a.details AS assetDetails, " +
            "a.baseRoomId AS assetBaseRoomId, br.name AS baseRoomName, a.currentRoomId AS assetCurrentRoomId " +
            "FROM assets a LEFT JOIN rooms br ON br.id = a.baseRoomId " +
            "WHERE a.currentRoomId = :roomId ORDER BY a.name"
    )
    fun observeRoomAssets(roomId: Long): Flow<List<RoomAssetTuple>>

    @Query("SELECT * FROM rooms WHERE id = :roomId")
    suspend fun getRoomById(roomId: Long): RoomEntity?
}

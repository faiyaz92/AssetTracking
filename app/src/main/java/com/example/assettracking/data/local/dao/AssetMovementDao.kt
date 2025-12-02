package com.example.assettracking.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.assettracking.data.local.entity.AssetMovementEntity
import com.example.assettracking.data.local.model.AssetMovementWithNamesTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetMovementDao {
    @Insert
    suspend fun insert(movement: AssetMovementEntity): Long

    @Query(
        "SELECT m.id AS movementId, m.assetId, a.name AS assetName, " +
            "m.fromRoomId, fr.name AS fromRoomName, " +
            "m.toRoomId, tr.name AS toRoomName, m.condition, m.timestamp " +
            "FROM asset_movements m " +
            "JOIN assets a ON a.id = m.assetId " +
            "LEFT JOIN rooms fr ON fr.id = m.fromRoomId " +
            "JOIN rooms tr ON tr.id = m.toRoomId " +
            "ORDER BY m.timestamp DESC"
    )
    fun observeMovements(): Flow<List<AssetMovementWithNamesTuple>>
}
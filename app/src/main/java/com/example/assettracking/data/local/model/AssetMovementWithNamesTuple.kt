package com.example.assettracking.data.local.model

import androidx.room.ColumnInfo

data class AssetMovementWithNamesTuple(
    @ColumnInfo(name = "movementId")
    val movementId: Long,
    @ColumnInfo(name = "assetId")
    val assetId: Long,
    @ColumnInfo(name = "assetName")
    val assetName: String,
    @ColumnInfo(name = "fromRoomId")
    val fromRoomId: Long?,
    @ColumnInfo(name = "fromRoomName")
    val fromRoomName: String?,
    @ColumnInfo(name = "toRoomId")
    val toRoomId: Long,
    @ColumnInfo(name = "toRoomName")
    val toRoomName: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
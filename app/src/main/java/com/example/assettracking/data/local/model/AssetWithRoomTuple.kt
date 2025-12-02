package com.example.assettracking.data.local.model

import androidx.room.ColumnInfo

data class AssetWithRoomTuple(
    @ColumnInfo(name = "assetId")
    val assetId: Long,
    @ColumnInfo(name = "assetCode")
    val assetCode: String,
    @ColumnInfo(name = "assetName")
    val assetName: String,
    @ColumnInfo(name = "assetDetails")
    val assetDetails: String?,
    @ColumnInfo(name = "assetCondition")
    val assetCondition: String?,
    @ColumnInfo(name = "assetBaseRoomId")
    val assetBaseRoomId: Long?,
    @ColumnInfo(name = "baseRoomName")
    val baseRoomName: String?,
    @ColumnInfo(name = "assetCurrentRoomId")
    val assetCurrentRoomId: Long?,
    @ColumnInfo(name = "currentRoomName")
    val currentRoomName: String?
)

data class RoomAssetTuple(
    @ColumnInfo(name = "assetId")
    val assetId: Long,
    @ColumnInfo(name = "assetCode")
    val assetCode: String,
    @ColumnInfo(name = "assetName")
    val assetName: String,
    @ColumnInfo(name = "assetDetails")
    val assetDetails: String?,
    @ColumnInfo(name = "assetCondition")
    val assetCondition: String?,
    @ColumnInfo(name = "assetBaseRoomId")
    val assetBaseRoomId: Long?,
    @ColumnInfo(name = "baseRoomName")
    val baseRoomName: String?,
    @ColumnInfo(name = "assetCurrentRoomId")
    val assetCurrentRoomId: Long
)

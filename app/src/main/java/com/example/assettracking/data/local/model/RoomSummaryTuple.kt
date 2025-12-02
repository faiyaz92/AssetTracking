package com.example.assettracking.data.local.model

import androidx.room.ColumnInfo

data class RoomSummaryTuple(
    @ColumnInfo(name = "roomId")
    val roomId: Long,
    @ColumnInfo(name = "roomName")
    val roomName: String,
    @ColumnInfo(name = "roomDescription")
    val roomDescription: String?,
    @ColumnInfo(name = "assetCount")
    val assetCount: Int
)

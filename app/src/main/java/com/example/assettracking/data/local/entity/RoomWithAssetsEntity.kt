package com.example.assettracking.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class RoomWithAssetsEntity(
    @Embedded
    val room: RoomEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "currentRoomId"
    )
    val assets: List<AssetEntity>
)

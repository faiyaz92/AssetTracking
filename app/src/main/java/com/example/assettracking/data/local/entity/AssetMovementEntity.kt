package com.example.assettracking.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "asset_movements",
    indices = [
        Index(value = ["assetId"]),
        Index(value = ["fromRoomId"]),
        Index(value = ["toRoomId"]),
        Index(value = ["timestamp"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromRoomId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["toRoomId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class AssetMovementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "assetId")
    val assetId: Long,
    @ColumnInfo(name = "fromRoomId")
    val fromRoomId: Long? = null,
    @ColumnInfo(name = "toRoomId")
    val toRoomId: Long,
    @ColumnInfo(name = "condition")
    val condition: String? = null,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
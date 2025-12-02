package com.example.assettracking.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assets",
    indices = [
        Index(value = ["baseRoomId"]),
        Index(value = ["currentRoomId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["baseRoomId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["currentRoomId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "details")
    val details: String? = null,
    @ColumnInfo(name = "condition")
    val condition: String? = null,
    @ColumnInfo(name = "baseRoomId")
    val baseRoomId: Long? = null,
    @ColumnInfo(name = "currentRoomId")
    val currentRoomId: Long? = null
)

package com.example.assettracking.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.assettracking.data.local.dao.AssetDao
import com.example.assettracking.data.local.dao.AssetMovementDao
import com.example.assettracking.data.local.dao.RoomDao
import com.example.assettracking.data.local.entity.AssetEntity
import com.example.assettracking.data.local.entity.AssetMovementEntity
import com.example.assettracking.data.local.entity.RoomEntity

@Database(
    entities = [RoomEntity::class, AssetEntity::class, AssetMovementEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AssetTrackingDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
    abstract fun assetDao(): AssetDao
    abstract fun assetMovementDao(): AssetMovementDao
}

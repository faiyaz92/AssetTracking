package com.example.assettracking.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.assettracking.data.local.dao.AssetDao
import com.example.assettracking.data.local.dao.AssetMovementDao
import com.example.assettracking.data.local.dao.LocationDao
import com.example.assettracking.data.local.entity.AssetEntity
import com.example.assettracking.data.local.entity.AssetMovementEntity
import com.example.assettracking.data.local.entity.LocationEntity

@Database(
    entities = [LocationEntity::class, AssetEntity::class, AssetMovementEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AssetTrackingDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun assetDao(): AssetDao
    abstract fun assetMovementDao(): AssetMovementDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Rename table from rooms to locations
                database.execSQL("ALTER TABLE rooms RENAME TO locations")
            }
        }
    }
}

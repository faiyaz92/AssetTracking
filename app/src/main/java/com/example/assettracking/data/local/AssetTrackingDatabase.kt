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
    version = 2,
    exportSchema = true
)
abstract class AssetTrackingDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
    abstract fun assetDao(): AssetDao
    abstract fun assetMovementDao(): AssetMovementDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to AssetEntity
                database.execSQL("ALTER TABLE assets ADD COLUMN baseRoomId INTEGER REFERENCES rooms(id)")
                database.execSQL("ALTER TABLE assets ADD COLUMN currentRoomId INTEGER REFERENCES rooms(id)")

                // Create AssetMovementEntity table
                database.execSQL("""
                    CREATE TABLE asset_movements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        assetId INTEGER NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
                        fromRoomId INTEGER REFERENCES rooms(id),
                        toRoomId INTEGER NOT NULL REFERENCES rooms(id),
                        timestamp INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}

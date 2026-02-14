package com.example.assettracking.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.assettracking.data.local.dao.AssetDao
import com.example.assettracking.data.local.dao.AssetMovementDao
import com.example.assettracking.data.local.dao.AuditDao
import com.example.assettracking.data.local.dao.LocationDao
import com.example.assettracking.data.local.entity.AssetEntity
import com.example.assettracking.data.local.entity.AssetMovementEntity
import com.example.assettracking.data.local.entity.AuditEntity
import com.example.assettracking.data.local.entity.LocationEntity

@Database(
    entities = [LocationEntity::class, AssetEntity::class, AssetMovementEntity::class, AuditEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AssetTrackingDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun assetDao(): AssetDao
    abstract fun assetMovementDao(): AssetMovementDao
    abstract fun auditDao(): AuditDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Rename table from rooms to locations
                database.execSQL("ALTER TABLE rooms RENAME TO locations")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add locationCode column to locations table
                database.execSQL("ALTER TABLE locations ADD COLUMN locationCode TEXT NOT NULL DEFAULT ''")
                // Generate location codes for existing locations
                // This will be handled by the application logic when locations are accessed
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add parentId column to locations table
                database.execSQL("ALTER TABLE locations ADD COLUMN parentId INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS audits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        includeChildren INTEGER NOT NULL,
                        locationId INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        finishedAt INTEGER,
                        FOREIGN KEY(locationId) REFERENCES locations(id) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_audits_locationId ON audits(locationId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_audits_createdAt ON audits(createdAt)")
            }
        }
    }
}

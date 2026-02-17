package com.example.assettracking.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.assettracking.data.local.dao.AssetDao
import com.example.assettracking.data.local.dao.AssetMovementDao
import com.example.assettracking.data.local.dao.AuditDao
import com.example.assettracking.data.local.dao.ChatMessageDao
import com.example.assettracking.data.local.dao.LocationDao
import com.example.assettracking.data.local.entity.AssetEntity
import com.example.assettracking.data.local.entity.AssetMovementEntity
import com.example.assettracking.data.local.entity.AuditEntity
import com.example.assettracking.data.local.entity.ChatMessageEntity
import com.example.assettracking.data.local.entity.LocationEntity

@Database(
    entities = [LocationEntity::class, AssetEntity::class, AssetMovementEntity::class, AuditEntity::class, ChatMessageEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AssetTrackingDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun assetDao(): AssetDao
    abstract fun assetMovementDao(): AssetMovementDao
    abstract fun auditDao(): AuditDao
    abstract fun chatMessageDao(): ChatMessageDao

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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        message_id TEXT NOT NULL,
                        text TEXT NOT NULL,
                        is_user INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        is_write_query INTEGER NOT NULL DEFAULT 0,
                        query_executed INTEGER NOT NULL DEFAULT 0,
                        original_query TEXT
                    )
                    """
                )
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

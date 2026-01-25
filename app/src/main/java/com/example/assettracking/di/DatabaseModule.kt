package com.example.assettracking.di

import android.content.Context
import androidx.room.Room
import com.example.assettracking.data.local.AssetTrackingDatabase
import com.example.assettracking.data.local.dao.AssetDao
import com.example.assettracking.data.local.dao.AssetMovementDao
import com.example.assettracking.data.local.dao.AuditDao
import com.example.assettracking.data.local.dao.LocationDao
import com.example.assettracking.data.repository.AssetMovementRepositoryImpl
import com.example.assettracking.domain.repository.AssetMovementRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AssetTrackingDatabase =
        Room.databaseBuilder(
            context,
            AssetTrackingDatabase::class.java,
            "asset-tracking.db"
        ).addMigrations(
            AssetTrackingDatabase.MIGRATION_1_2,
            AssetTrackingDatabase.MIGRATION_2_3,
            AssetTrackingDatabase.MIGRATION_3_4,
            AssetTrackingDatabase.MIGRATION_4_5
        ).build()

    @Provides
    fun provideLocationDao(database: AssetTrackingDatabase): LocationDao = database.locationDao()

    @Provides
    fun provideAssetDao(database: AssetTrackingDatabase): AssetDao = database.assetDao()

    @Provides
    fun provideAssetMovementDao(database: AssetTrackingDatabase): AssetMovementDao = database.assetMovementDao()

    @Provides
    fun provideAuditDao(database: AssetTrackingDatabase): AuditDao = database.auditDao()
}

package com.example.assettracking.di

import android.content.Context
import androidx.room.Room
import com.example.assettracking.data.local.AssetTrackingDatabase
import com.example.assettracking.data.local.dao.AssetDao
import com.example.assettracking.data.local.dao.AssetMovementDao
import com.example.assettracking.data.local.dao.RoomDao
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
        ).build()

    @Provides
    fun provideRoomDao(database: AssetTrackingDatabase): RoomDao = database.roomDao()

    @Provides
    fun provideAssetDao(database: AssetTrackingDatabase): AssetDao = database.assetDao()

    @Provides
    fun provideAssetMovementDao(database: AssetTrackingDatabase): AssetMovementDao = database.assetMovementDao()
}

package com.example.assettracking.di

import com.example.assettracking.data.repository.AssetMovementRepositoryImpl
import com.example.assettracking.data.repository.AssetRepositoryImpl
import com.example.assettracking.data.repository.AuditRepositoryImpl
import com.example.assettracking.data.repository.LocationRepositoryImpl
import com.example.assettracking.domain.repository.AssetMovementRepository
import com.example.assettracking.domain.repository.AssetRepository
import com.example.assettracking.domain.repository.AuditRepository
import com.example.assettracking.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindAssetRepository(
        impl: AssetRepositoryImpl
    ): AssetRepository

    @Binds
    @Singleton
    abstract fun bindAssetMovementRepository(
        impl: AssetMovementRepositoryImpl
    ): AssetMovementRepository

    @Binds
    @Singleton
    abstract fun bindAuditRepository(
        impl: AuditRepositoryImpl
    ): AuditRepository
}

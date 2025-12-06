package com.example.assettracking.di

import com.example.assettracking.util.C72RfidReader
import com.example.assettracking.util.RfidReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RfidModule {

    @Binds
    @Singleton
    abstract fun bindRfidReader(c72RfidReader: C72RfidReader): RfidReader
}
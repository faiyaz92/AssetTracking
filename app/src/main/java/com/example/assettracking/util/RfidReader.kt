package com.example.assettracking.util

interface RfidReader {
    fun initialize(): Boolean
    suspend fun readTag(): String?
    fun writeTag(assetId: String): Boolean
    fun killTag(password: String): Boolean
    fun startInventory(): kotlinx.coroutines.flow.Flow<List<String>>
    suspend fun inventory(): List<String>
    suspend fun singleInventory(): String?
    suspend fun bulkInventory(onTagFound: (String) -> Unit, durationMs: Long = 3000L)
    fun stopBulkInventory(): Boolean
    suspend fun close()
    fun isAvailable(): Boolean
}
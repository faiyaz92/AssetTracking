package com.example.assettracking.util

import kotlinx.coroutines.flow.Flow

/**
 * Interface for RFID reader implementations
 * Supports both NFC and UHF RFID technologies
 */
interface RfidReader {

    /**
     * Initialize the RFID reader
     * @return true if initialization successful, false otherwise
     */
    fun initialize(): Boolean

    /**
     * Read a single RFID tag
     * @return asset ID from the tag, or null if no tag found or error
     */
    fun readTag(): String?

    /**
     * Write asset ID to an RFID tag
     * @param assetId the asset ID to write (e.g., "000001")
     * @return true if write successful, false otherwise
     */
    fun writeTag(assetId: String): Boolean

    /**
     * Kill (permanently disable) an RFID tag
     * @param password access password for the tag
     * @return true if kill successful, false otherwise
     */
    fun killTag(password: String): Boolean

    /**
     * Start inventory mode to read multiple tags
     * @return Flow of lists of asset IDs found
     */
    fun startInventory(): Flow<List<String>>

    /**
     * Perform inventory scan and return list of asset IDs
     * @return list of asset IDs found
     */
    suspend fun inventory(): List<String>

    /**
     * Close the RFID reader and release resources
     */
    fun close()

    /**
     * Check if the RFID reader is available
     * @return true if reader is available, false otherwise
     */
    fun isAvailable(): Boolean
}
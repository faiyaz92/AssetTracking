package com.example.assettracking.util

import android.content.Context
import android.util.Log
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.entity.InventoryParameter
import com.rscja.deviceapi.entity.UHFTAGInfo
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom exception for RFID hardware-related errors
 */
class RfidHardwareException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    companion object {
        fun hardwareNotFound() = RfidHardwareException("RFID hardware not found. Please ensure the device has RFID capabilities.")
        fun hardwareNotInitialized() = RfidHardwareException("RFID hardware failed to initialize. Check device connections.")
        fun permissionDenied() = RfidHardwareException("Permission denied for RFID hardware access.")
        fun connectionFailed() = RfidHardwareException("Failed to connect to RFID module. Check UART connection.")
        fun scanTimeout() = RfidHardwareException("RFID scan timeout. No tags found in scan area.")
        
        private fun sanitizeErrorMessage(message: String?): String {
            return when {
                message.isNullOrBlank() -> "Unknown hardware error occurred"
                message.contains("null", ignoreCase = true) ||
                message.contains("NullPointerException", ignoreCase = true) ||
                message.contains("String.contains", ignoreCase = true) -> "RFID hardware communication error. Please check device connections and try again."
                else -> message
            }
        }
        
        fun unknownError(message: String?) = RfidHardwareException("RFID error: ${sanitizeErrorMessage(message)}")
    }
}

@Singleton
class C72RfidReader @Inject constructor(
    @ApplicationContext private val context: Context
) : RfidReader {

    private var uhfReader: RFIDWithUHFUART? = null // Real Chainway RFID reader
    private val TAG = "C72RfidReader"
    private val USE_REAL_SDK = true // Set to true when SDK is available

    override fun initialize(): Boolean {
        return try {
            if (USE_REAL_SDK) {
                // Real SDK implementation - matching demo pattern
                if (uhfReader == null) {
                    uhfReader = RFIDWithUHFUART.getInstance()
                }
                // Initialize the reader - in demo this is done in AsyncTask
                // We'll do it synchronously here for simplicity
                val result = uhfReader?.init(context) ?: false
                if (result) {
                    Log.d(TAG, "UHF Reader initialized successfully")
                    true
                } else {
                    Log.e(TAG, "Failed to initialize UHF Reader - Hardware not available or not connected")
                    throw RfidHardwareException.hardwareNotInitialized()
                }
            } else {
                // Mock implementation
                Log.d(TAG, "Mock UHF Reader initialized successfully")
                uhfReader = null // Mock doesn't need reader object
                true
            }
        } catch (e: RfidHardwareException) {
            // Re-throw our custom exceptions
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing UHF Reader: ${e.message}", e)
            // Check for common hardware-related exceptions and throw specific ones
            when {
                e.message?.contains("device", ignoreCase = true) == true ||
                e.message?.contains("hardware", ignoreCase = true) == true -> {
                    throw RfidHardwareException.hardwareNotFound()
                }
                e.message?.contains("permission", ignoreCase = true) == true -> {
                    throw RfidHardwareException.permissionDenied()
                }
                e.message?.contains("connect", ignoreCase = true) == true ||
                e.message?.contains("uart", ignoreCase = true) == true -> {
                    throw RfidHardwareException.connectionFailed()
                }
                else -> {
                    throw RfidHardwareException.unknownError(e.message)
                }
            }
        }
    }

    override suspend fun readTag(): String? {
        return try {
            if (USE_REAL_SDK) {
                // Real SDK - use inventorySingleTag to read a tag (matching demo)
                val tagInfo = withContext(Dispatchers.IO) {
                    uhfReader?.inventorySingleTag()
                }
                if (tagInfo != null && tagInfo.epc != null) {
                    val assetId = tagInfo.epc
                    Log.d(TAG, "Read tag successfully: $assetId")
                    assetId
                } else {
                    Log.d(TAG, "No tag found")
                    null
                }
            } else {
                // Mock implementation
                Log.d(TAG, "Mock readTag - no tag found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading tag: ${e.message}", e)
            throw RfidHardwareException.unknownError(e.message)
        }
    }

    override fun writeTag(assetId: String): Boolean {
        return try {
            if (USE_REAL_SDK) {
                // Real SDK implementation - write to EPC bank (bank 1)
                // Based on demo: writeData(password, bank, ptr, len, data)
                val result = uhfReader?.writeData("00000000",  // No password
                    RFIDWithUHFUART.Bank_EPC,  // Write to EPC bank
                    2,  // Start at word 2 (after PC and EPC length)
                    assetId.length / 4,  // Length in words (4 hex chars = 1 word)
                    assetId  // The asset ID data
                ) ?: false
                if (result) {
                    Log.d(TAG, "Write tag successfully: $assetId")
                    true
                } else {
                    Log.e(TAG, "Failed to write tag")
                    false
                }
            } else {
                // Mock implementation
                Log.d(TAG, "Mock writeTag successful: $assetId")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing tag: ${e.message}", e)
            throw RfidHardwareException.unknownError(e.message)
        }
    }

    override fun killTag(password: String): Boolean {
        return try {
            if (USE_REAL_SDK) {
                // Real SDK implementation - kill tag
                val result = uhfReader?.killTag(password) ?: false
                if (result) {
                    Log.d(TAG, "Kill tag successfully")
                    true
                } else {
                    Log.e(TAG, "Failed to kill tag")
                    false
                }
            } else {
                // Mock implementation
                Log.d(TAG, "Mock killTag successful")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error killing tag: ${e.message}", e)
            throw RfidHardwareException.unknownError(e.message)
        }
    }

    override fun startInventory(): Flow<List<String>> = flow {
        try {
            if (USE_REAL_SDK) {
                // Real SDK implementation - start continuous inventory
                uhfReader?.startInventoryTag() ?: run {
                    Log.e(TAG, "Failed to start inventory - reader not initialized")
                    emit(emptyList())
                    return@flow
                }

                // Collect tags for a short period
                val foundTags = mutableSetOf<String>()
                val startTime = System.currentTimeMillis()
                val durationMs = 2000L // 2 seconds

                while (System.currentTimeMillis() - startTime < durationMs) {
                    try {
                        val tagInfo = uhfReader?.inventorySingleTag()
                        if (tagInfo != null && tagInfo.epc != null) {
                            foundTags.add(tagInfo.epc)
                            emit(foundTags.toList())
                        }
                        delay(100) // Small delay between scans
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during inventory scan: ${e.message}", e)
                        break
                    }
                }

                // Stop inventory
                uhfReader?.stopInventory()

                Log.d(TAG, "Inventory completed, found ${foundTags.size} unique tags")
            } else {
                // Mock implementation
                Log.d(TAG, "Mock startInventory")
                emit(listOf("MOCK001", "MOCK002"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in startInventory: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun inventory(): List<String> {
        return try {
            if (USE_REAL_SDK) {
                Log.e(TAG, "Reader not available for inventory - Hardware initialization failed")
                // Real SDK implementation
                val foundTags = mutableListOf<String>()
                
                // Perform a single inventory scan (not continuous bulk scanning)
                val startTime = System.currentTimeMillis()
                val scanDuration = 3000L // 3 seconds
                
                while (System.currentTimeMillis() - startTime < scanDuration) {
                    try {
                        val tagInfo = withContext(Dispatchers.IO) {
                            uhfReader?.inventorySingleTag()
                        }
                        if (tagInfo != null && tagInfo.epc != null && !foundTags.contains(tagInfo.epc)) {
                            foundTags.add(tagInfo.epc)
                            Log.d(TAG, "Found tag during inventory: ${tagInfo.epc}")
                        }
                        delay(200) // Delay between individual scans
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during inventory scan iteration: ${e.message}", e)
                        break
                    }
                }
                
                Log.d(TAG, "Single inventory scan found ${foundTags.size} tags: $foundTags")
                foundTags
            } else {
                // Mock single inventory
                Log.d(TAG, "Mock single inventory scan")
                listOf("MOCK001", "MOCK002", "MOCK003")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in inventory: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun singleInventory(): String? {
        return try {
            if (USE_REAL_SDK) {
                Log.e(TAG, "Reader not available for single inventory - Hardware initialization failed")
                // Real SDK implementation
                val tagInfo = withContext(Dispatchers.IO) {
                    uhfReader?.inventorySingleTag()
                }
                if (tagInfo != null && tagInfo.epc != null) {
                    val assetId = tagInfo.epc
                    Log.d(TAG, "Single inventory found tag: $assetId")
                    assetId
                } else {
                    Log.d(TAG, "Single inventory - no tag found")
                    null
                }
            } else {
                // Mock single inventory
                Log.d(TAG, "Mock single inventory scan")
                "MOCK001"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in single inventory: ${e.message}", e)
            null
        }
    }

    override suspend fun bulkInventory(onTagFound: (String) -> Unit, durationMs: Long) {
        try {
            if (USE_REAL_SDK) {
                Log.e(TAG, "Reader not available for bulk inventory - Hardware initialization failed")
                // Real SDK implementation with callback
                uhfReader?.setInventoryCallback(object : IUHFInventoryCallback {
                    override fun callback(tagInfo: UHFTAGInfo) {
                        if (tagInfo.epc != null) {
                            Log.d(TAG, "Bulk inventory found tag: ${tagInfo.epc}")
                            onTagFound(tagInfo.epc)
                        }
                    }
                })

                // Start bulk inventory
                val inventoryParam = InventoryParameter()
                withContext(Dispatchers.IO) {
                    uhfReader?.startInventoryTag(inventoryParam)
                }

                // Wait for the specified duration
                delay(durationMs)

                // Stop bulk inventory
                withContext(Dispatchers.IO) {
                    uhfReader?.stopInventory()
                }
                Log.d(TAG, "Bulk inventory completed")
            } else {
                // Mock bulk inventory
                Log.d(TAG, "Mock bulk inventory started")
                val mockTags = listOf("MOCK001", "MOCK002", "MOCK003")
                for (tag in mockTags) {
                    delay(500) // Simulate finding tags over time
                    onTagFound(tag)
                }
                Log.d(TAG, "Mock bulk inventory completed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in bulk inventory: ${e.message}", e)
        }
    }

    override fun stopBulkInventory(): Boolean {
        return try {
            if (USE_REAL_SDK) {
                // Real SDK implementation
                val result = uhfReader?.stopInventory() ?: false
                if (result) {
                    Log.d(TAG, "Bulk inventory stopped successfully")
                    true
                } else {
                    Log.e(TAG, "Failed to stop bulk inventory")
                    false
                }
            } else {
                // Mock implementation
                Log.d(TAG, "Mock bulk inventory stopped")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping bulk inventory: ${e.message}", e)
            false
        }
    }

    override suspend fun close() {
        try {
            if (USE_REAL_SDK) {
                // Real SDK implementation
                withContext(Dispatchers.IO) {
                    uhfReader?.free()
                }
                uhfReader = null
                Log.d(TAG, "UHF Reader closed successfully")
            } else {
                // Mock implementation
                Log.d(TAG, "Mock UHF Reader closed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing UHF Reader: ${e.message}", e)
        }
    }

    override fun isAvailable(): Boolean {
        return try {
            if (USE_REAL_SDK) {
                // Real SDK implementation - check if reader is available
                // TODO: Check correct method name in Chainway SDK
                uhfReader != null // Placeholder check
            } else {
                // Mock implementation
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking reader availability: ${e.message}", e)
            false
        }
    }

    // Public wrapper methods for direct SDK access (used by demo screens)
    fun setInventoryCallback(callback: IUHFInventoryCallback?) {
        uhfReader?.setInventoryCallback(callback)
    }

    fun startInventoryTag(): Boolean {
        return uhfReader?.startInventoryTag() ?: false
    }

    fun stopInventory(): Boolean {
        return uhfReader?.stopInventory() ?: false
    }

    fun readData(
        epcData: String?,
        memBank: Int,
        address: Int,
        wordCount: Int
    ): String? {
        return uhfReader?.readData(epcData, memBank, address, wordCount)
    }

    fun writeData(
        epcData: String?,
        memBank: Int,
        address: Int,
        wordCount: Int,
        data: String?
    ): Boolean {
        return uhfReader?.writeData(epcData, memBank, address, wordCount, data) ?: false
    }
}
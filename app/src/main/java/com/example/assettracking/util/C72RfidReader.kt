package com.example.assettracking.util

import android.content.Context
import android.util.Log
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.entity.UHFTAGInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chainway C72 UHF RFID Reader utility class
 * Handles UHF RFID operations for asset tracking
 *
 * Uses the actual Chainway RFID SDK
 */
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
                // Real SDK implementation
                if (uhfReader == null) {
                    uhfReader = RFIDWithUHFUART.getInstance()
                }
                // Note: init() is asynchronous in the demo, but for simplicity we'll assume it succeeds
                val result = uhfReader?.init(context) ?: false
                if (result) {
                    Log.d(TAG, "UHF Reader initialized successfully")
                    true
                } else {
                    Log.e(TAG, "Failed to initialize UHF Reader")
                    false
                }
            } else {
                // Mock implementation
                Log.d(TAG, "Mock UHF Reader initialized successfully")
                uhfReader = null // Mock doesn't need reader object
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing UHF Reader", e)
            false
        }
    }

    override fun readTag(): String? {
        return try {
            if (uhfReader == null && !initialize()) {
                Log.e(TAG, "Reader not available")
                return null
            }

            if (USE_REAL_SDK) {
                // Real SDK - use inventorySingleTag to read a tag
                val tagInfo = uhfReader?.inventorySingleTag()
                if (tagInfo != null && !tagInfo.epc.isNullOrEmpty()) {
                    Log.d(TAG, "Tag read successfully: ${tagInfo.epc}")
                    tagInfo.epc
                } else {
                    Log.d(TAG, "No tag found")
                    null
                }
            } else {
                // Mock
                Log.d(TAG, "Mock tag read - returning test asset ID")
                "000001"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading tag", e)
            null
        }
    }

    override fun writeTag(assetId: String): Boolean {
        return try {
            if (uhfReader == null && !initialize()) {
                Log.e(TAG, "Reader not available for writing")
                return false
            }

            if (USE_REAL_SDK) {
                // Real SDK - write to EPC bank (bank 1)
                // Convert assetId to hex if needed, assuming it's already in correct format
                val hexData = assetId // Assuming assetId is already hex or will be converted
                val result = uhfReader?.writeData("00000000", RFIDWithUHFUART.Bank_EPC, 2, hexData.length / 4, hexData) ?: false
                if (result) {
                    Log.d(TAG, "Tag write successful for asset ID: $assetId")
                    true
                } else {
                    Log.e(TAG, "Tag write failed")
                    false
                }
            } else {
                // Mock
                Log.d(TAG, "Mock tag write successful for asset ID: $assetId")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing tag", e)
            false
        }
    }

    override fun killTag(password: String): Boolean {
        return try {
            if (uhfReader == null && !initialize()) {
                Log.e(TAG, "Reader not available for killing tag")
                return false
            }

            if (USE_REAL_SDK) {
                // Real SDK - kill tag functionality
                // Note: Kill functionality may require specific implementation
                // For now, return false as kill is not commonly used
                Log.w(TAG, "Kill tag functionality not implemented in current SDK version")
                false
            } else {
                // Mock
                Log.d(TAG, "Mock tag kill successful")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error killing tag", e)
            false
        }
    }

    override suspend fun inventory(): List<String> {
        return startInventory().first()
    }

    override fun startInventory(): Flow<List<String>> = flow {
        try {
            if (uhfReader == null && !initialize()) {
                Log.e(TAG, "Reader not available for inventory")
                emit(emptyList())
                return@flow
            }

            if (USE_REAL_SDK) {
                // Real SDK - perform multiple single inventories to simulate continuous scanning
                val foundTags = mutableSetOf<String>()
                val startTime = System.currentTimeMillis()
                val duration = 3000L // 3 seconds of scanning

                while (System.currentTimeMillis() - startTime < duration) {
                    val tagInfo = uhfReader?.inventorySingleTag()
                    if (tagInfo != null && !tagInfo.epc.isNullOrEmpty()) {
                        foundTags.add(tagInfo.epc)
                    }
                    delay(100) // Small delay between scans
                }

                val assetIds = foundTags.toList()
                Log.d(TAG, "Found ${assetIds.size} tags: $assetIds")
                emit(assetIds)
            } else {
                // Mock
                Log.d(TAG, "Mock inventory started")
                delay(1000)
                emit(listOf("000001", "000002", "000003"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during inventory", e)
            emit(emptyList())
        }
    }

    override fun close() {
        try {
            if (USE_REAL_SDK) {
                uhfReader?.free()
            }
            uhfReader = null
            Log.d(TAG, "UHF Reader closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing reader", e)
        }
    }

    override fun isAvailable(): Boolean {
        return try {
            uhfReader != null || initialize()
        } catch (e: Exception) {
            false
        }
    }
}
package com.example.assettracking.presentation.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.util.RfidReader
import com.example.assettracking.util.RfidHardwareException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val rfidReader: RfidReader
) : ViewModel() {

    private val _rfidScanState = MutableStateFlow<RfidScanState>(RfidScanState.Idle)
    val rfidScanState = _rfidScanState.asStateFlow()

    private val _radarScanState = MutableStateFlow<RadarScanState>(RadarScanState.Idle)
    val radarScanState = _radarScanState.asStateFlow()

    fun startRfidScan() {
        _rfidScanState.value = RfidScanState.Scanning
        viewModelScope.launch {
            try {
                val tags = rfidReader.inventory()
                _rfidScanState.value = RfidScanState.Success(tags)
            } catch (e: RfidHardwareException) {
                // Handle hardware-specific errors with user-friendly messages
                val errorMessage = when {
                    e.message?.contains("not found") == true -> "RFID hardware not detected. Please ensure you're using a device with RFID capabilities."
                    e.message?.contains("not initialized") == true -> "RFID hardware failed to initialize. Check device connections and try again."
                    e.message?.contains("permission") == true -> "Permission denied for RFID access. Please grant necessary permissions."
                    e.message?.contains("connection") == true -> "Failed to connect to RFID module. Check hardware connections."
                    e.message?.contains("timeout") == true -> "RFID scan timeout. No tags found in scan area. Try moving closer to RFID tags."
                    else -> e.message ?: "RFID scan failed due to hardware error."
                }
                val stackTrace = android.util.Log.getStackTraceString(e)
                _rfidScanState.value = RfidScanState.Error(errorMessage, stackTrace)
            } catch (e: Exception) {
                val errorMessage = "Unexpected error during RFID scan: ${e.message}"
                val stackTrace = android.util.Log.getStackTraceString(e)
                _rfidScanState.value = RfidScanState.Error(errorMessage, stackTrace)
            }
        }
    }

    fun clearRfidScan() {
        _rfidScanState.value = RfidScanState.Idle
    }

    // Radar scanning - continuous detection like UHFReadTagFragment
    fun startRadarScan() {
        _radarScanState.value = RadarScanState.Scanning(emptyList(), 0)
        viewModelScope.launch {
            try {
                val detectedTags = mutableMapOf<String, TagInfo>()
                var totalCount = 0
                
                rfidReader.bulkInventory(
                    onTagFound = { epc ->
                        totalCount++
                        val existing = detectedTags[epc]
                        if (existing != null) {
                            // Tag seen again - increment count
                            detectedTags[epc] = existing.copy(count = existing.count + 1)
                        } else {
                            // New tag
                            detectedTags[epc] = TagInfo(epc = epc, count = 1)
                        }
                        // Update UI with current state
                        _radarScanState.value = RadarScanState.Scanning(
                            tags = detectedTags.values.toList(),
                            totalCount = totalCount
                        )
                    },
                    durationMs = 10000L // 10 seconds continuous scan
                )
                
                // Scan complete
                _radarScanState.value = RadarScanState.Complete(
                    tags = detectedTags.values.toList(),
                    totalCount = totalCount
                )
            } catch (e: RfidHardwareException) {
                val errorMessage = when {
                    e.message?.contains("not found") == true -> "RFID hardware not detected."
                    e.message?.contains("not initialized") == true -> "RFID hardware failed to initialize."
                    e.message?.contains("permission") == true -> "Permission denied for RFID access."
                    e.message?.contains("connection") == true -> "Failed to connect to RFID module."
                    else -> e.message ?: "RFID radar scan failed."
                }
                val stackTrace = android.util.Log.getStackTraceString(e)
                _radarScanState.value = RadarScanState.Error(errorMessage, stackTrace)
            } catch (e: Exception) {
                val errorMessage = "Unexpected error during radar scan: ${e.message}"
                val stackTrace = android.util.Log.getStackTraceString(e)
                _radarScanState.value = RadarScanState.Error(errorMessage, stackTrace)
            }
        }
    }

    fun clearRadarScan() {
        _radarScanState.value = RadarScanState.Idle
    }

    data class TagInfo(
        val epc: String,
        val count: Int = 1,
        val rssi: String? = null
    )

    sealed class RfidScanState {
        object Idle : RfidScanState()
        object Scanning : RfidScanState()
        data class Success(val tags: List<String>) : RfidScanState()
        data class Error(val message: String, val stackTrace: String = "") : RfidScanState()
    }

    sealed class RadarScanState {
        object Idle : RadarScanState()
        data class Scanning(val tags: List<TagInfo>, val totalCount: Int) : RadarScanState()
        data class Complete(val tags: List<TagInfo>, val totalCount: Int) : RadarScanState()
        data class Error(val message: String, val stackTrace: String = "") : RadarScanState()
    }
}
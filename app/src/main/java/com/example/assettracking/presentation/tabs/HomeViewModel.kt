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

    sealed class RfidScanState {
        object Idle : RfidScanState()
        object Scanning : RfidScanState()
        data class Success(val tags: List<String>) : RfidScanState()
        data class Error(val message: String, val stackTrace: String = "") : RfidScanState()
    }
}
package com.example.assettracking.presentation.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.util.RfidReader
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
            } catch (e: Exception) {
                _rfidScanState.value = RfidScanState.Error(e.message ?: "Scan failed")
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
        data class Error(val message: String) : RfidScanState()
    }
}
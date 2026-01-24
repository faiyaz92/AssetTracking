package com.example.assettracking.presentation.locationdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.domain.repository.LocationRepository
import com.example.assettracking.domain.usecase.CreateMovementUseCase
import com.example.assettracking.domain.usecase.DetachAssetFromRoomUseCase
import com.example.assettracking.domain.usecase.FindAssetByIdUseCase
import com.example.assettracking.domain.usecase.ObserveRoomDetailUseCase
import com.example.assettracking.domain.usecase.UpdateCurrentRoomUseCase
import com.example.assettracking.presentation.common.UiMessage
import com.example.assettracking.util.RfidReader
import com.example.assettracking.util.RfidHardwareException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOCATION_ID_KEY = "locationId"

@HiltViewModel
class LocationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeRoomDetailUseCase: ObserveRoomDetailUseCase,
    private val detachAssetFromRoomUseCase: DetachAssetFromRoomUseCase,
    private val findAssetByIdUseCase: FindAssetByIdUseCase,
    private val updateCurrentRoomUseCase: UpdateCurrentRoomUseCase,
    private val createMovementUseCase: CreateMovementUseCase,
    // private val createRoomUseCase: CreateRoomUseCase,
    private val rfidReader: RfidReader,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private var locationId: Long? = null

    private val _uiState = MutableStateFlow(LocationDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun setLocationIdentifier(identifier: String) {
        viewModelScope.launch {
            locationId = resolveLocationIdentifier(identifier)
            if (locationId != null) {
                observeLocationDetails()
            } else {
                _uiState.update {
                    it.copy(
                        message = UiMessage("Location not found: $identifier"),
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun resolveLocationIdentifier(identifier: String): Long? {
        // Try parsing as Long first (ID)
        identifier.toLongOrNull()?.let { id ->
            // Verify the location exists
            return if (locationRepository.getLocationById(id) != null) id else null
        }

        // Try as location code
        return locationRepository.getLocationByCode(identifier)?.id
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun toggleGrouping() {
        _uiState.update { it.copy(isGrouped = !it.isGrouped) }
    }

    fun assignAsset(assetCode: String) {
        val assetId = assetCode.toLongOrNull()
        if (assetId == null) {
            _uiState.update { it.copy(message = UiMessage("Invalid barcode")) }
            return
        }
        viewModelScope.launch {
            val currentLocationId = locationId
            if (currentLocationId == null) {
                _uiState.update { it.copy(message = UiMessage("Location not loaded yet")) }
                return@launch
            }

            // First, find the asset to get its current location
            val asset = findAssetByIdUseCase(assetId)
            if (asset == null) {
                _uiState.update { it.copy(message = UiMessage("Asset not found")) }
                return@launch
            }

            // Update current room
            val updateResult = updateCurrentRoomUseCase(asset.id, currentLocationId)
            if (updateResult.isFailure) {
                _uiState.update { it.copy(message = UiMessage("Failed to update asset location")) }
                return@launch
            }

            // Log movement for audit trail
            try {
                createMovementUseCase(asset.id, asset.currentRoomId, currentLocationId)
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(message = UiMessage(error.message ?: "Asset location updated but movement not logged"))
                }
            }
        }
    }

    fun detachAsset(assetId: Long) {
        viewModelScope.launch {
            val result = detachAssetFromRoomUseCase(assetId)
            result.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to detach asset")) }
            }
        }
    }

    fun createChildLocation(name: String, description: String?) {
        viewModelScope.launch {
            val currentLocationId = locationId
            if (currentLocationId == null) {
                _uiState.update { it.copy(message = UiMessage("Current location not loaded")) }
                return@launch
            }

            try {
                val normalized = name.trim()
                if (normalized.isBlank()) {
                    _uiState.update { it.copy(message = UiMessage("Location name required")) }
                    return@launch
                }
                locationRepository.createLocation(normalized, description?.trim(), currentLocationId)
                _uiState.update { it.copy(message = UiMessage("Child location created successfully")) }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = UiMessage(e.message ?: "Unable to create child location")) }
            }
        }
    }

    fun startRfidScan() {
        _uiState.update { it.copy(isRfidScanning = true, scannedRfidTags = emptyList()) }
        viewModelScope.launch {
            try {
                val tags = rfidReader.inventory()
                _uiState.update { it.copy(scannedRfidTags = tags, isRfidScanning = false) }
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
                _uiState.update {
                    it.copy(
                        message = UiMessage(errorMessage, stackTrace),
                        isRfidScanning = false
                    )
                }
            } catch (e: Exception) {
                val errorMessage = "Unexpected error during RFID scan: ${e.message}"
                val stackTrace = android.util.Log.getStackTraceString(e)
                _uiState.update {
                    it.copy(
                        message = UiMessage(errorMessage, stackTrace),
                        isRfidScanning = false
                    )
                }
            }
        }
    }

    fun startSingleRfidScan() {
        _uiState.update { it.copy(isRfidScanning = true, scannedRfidTags = emptyList()) }
        viewModelScope.launch {
            try {
                val tag = rfidReader.singleInventory()
                val tags = if (tag != null) listOf(tag) else emptyList()
                _uiState.update { it.copy(scannedRfidTags = tags, isRfidScanning = false) }
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
                _uiState.update {
                    it.copy(
                        message = UiMessage(errorMessage, stackTrace),
                        isRfidScanning = false
                    )
                }
            } catch (e: Exception) {
                val errorMessage = "Unexpected error during single RFID scan: ${e.message}"
                val stackTrace = android.util.Log.getStackTraceString(e)
                _uiState.update {
                    it.copy(
                        message = UiMessage(errorMessage, stackTrace),
                        isRfidScanning = false
                    )
                }
            }
        }
    }

    fun startBulkRfidScan() {
        _uiState.update { it.copy(
            isRfidScanning = true, 
            scannedRfidTags = emptyList(),
            scanCompleted = false,
            totalScanned = 0,
            isScanStoppable = true // Enable manual stop
        ) }
        viewModelScope.launch {
            try {
                val foundTags = mutableSetOf<String>() // Use Set to avoid duplicates
                rfidReader.bulkInventory(
                    onTagFound = { tag ->
                        foundTags.add(tag)
                        // Update UI with each unique tag found + total count
                        _uiState.update { it.copy(
                            scannedRfidTags = foundTags.toList(),
                            totalScanned = foundTags.size
                        ) }
                    },
                    durationMs = 30000L // 30 seconds max, but user can stop anytime
                )
                // Mark scan as completed (either timed out or manually stopped)
                _uiState.update { it.copy(
                    isRfidScanning = false,
                    scanCompleted = true,
                    isScanStoppable = false
                ) }
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
                _uiState.update {
                    it.copy(
                        message = UiMessage(errorMessage, stackTrace),
                        isRfidScanning = false
                    )
                }
            } catch (e: Exception) {
                val errorMessage = "Unexpected error during bulk RFID scan: ${e.message}"
                val stackTrace = android.util.Log.getStackTraceString(e)
                _uiState.update {
                    it.copy(
                        message = UiMessage(errorMessage, stackTrace),
                        isRfidScanning = false
                    )
                }
            }
        }
    }

    fun assignBulkAssets(assetCodes: List<String>) {
        val currentLocationId = locationId
        if (currentLocationId == null) {
            _uiState.update { it.copy(message = UiMessage("Location not loaded yet")) }
            return
        }

        viewModelScope.launch {
            var successCount = 0
            var failureCount = 0
            var ignoredCount = 0
            val failedAssets = mutableListOf<String>()
            val ignoredAssets = mutableListOf<String>()
            
            // Process each asset sequentially to ensure DB operations complete properly
            assetCodes.forEach { assetCode ->
                val assetId = assetCode.toLongOrNull()
                if (assetId == null) {
                    ignoredCount++
                    ignoredAssets.add(assetCode)
                    return@forEach
                }
                
                try {
                    // Find the asset - THIS FILTERS ONLY EXISTING ASSETS IN DB
                    val asset = findAssetByIdUseCase(assetId)
                    if (asset == null) {
                        // Asset not in our database - IGNORE IT
                        ignoredCount++
                        ignoredAssets.add(assetCode)
                        return@forEach
                    }

                    // Update current room
                    val updateResult = updateCurrentRoomUseCase(asset.id, currentLocationId)
                    if (updateResult.isFailure) {
                        failureCount++
                        failedAssets.add(assetCode)
                        return@forEach
                    }

                    // Log movement for audit trail
                    try {
                        createMovementUseCase(asset.id, asset.currentRoomId, currentLocationId)
                        successCount++
                    } catch (error: Exception) {
                        // Movement logged failed but location updated
                        successCount++
                    }
                } catch (e: Exception) {
                    failureCount++
                    failedAssets.add(assetCode)
                }
            }
            
            // Show summary message
            val message = buildString {
                append("Bulk assignment complete:\n")
                append("✓ $successCount assigned")
                if (ignoredCount > 0) {
                    append("\n⊗ $ignoredCount ignored (not in database)")
                }
                if (failureCount > 0) {
                    append("\n✗ $failureCount failed")
                    if (failedAssets.isNotEmpty()) {
                        append(" (")
                        append(failedAssets.take(3).joinToString(", "))
                        if (failedAssets.size > 3) {
                            append(" and ${failedAssets.size - 3} more")
                        }
                        append(")")
                    }
                }
            }
            
            _uiState.update { it.copy(message = UiMessage(message)) }
        }
    }

    fun stopBulkRfidScan() {
        viewModelScope.launch {
            try {
                rfidReader.stopBulkInventory()
                _uiState.update { it.copy(
                    isRfidScanning = false,
                    scanCompleted = true,
                    isScanStoppable = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isRfidScanning = false,
                    isScanStoppable = false
                ) }
            }
        }
    }

    fun clearRfidScan() {
        _uiState.update { it.copy(
            scannedRfidTags = emptyList(), 
            isRfidScanning = false,
            scanCompleted = false,
            totalScanned = 0,
            isScanStoppable = false
        ) }
    }

    private fun observeLocationDetails() {
        val id = locationId ?: return
        observeRoomDetailUseCase(id)
            .onEach { detail ->
                _uiState.update { state ->
                    state.copy(
                        locationDetail = detail,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}

package com.example.assettracking.presentation.tabs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.domain.usecase.AssignAssetToRoomWithConditionUseCase
import com.example.assettracking.domain.usecase.CreateMovementUseCase
import com.example.assettracking.domain.usecase.FindAssetByIdUseCase
import com.example.assettracking.domain.usecase.ObserveRoomsUseCase
import com.example.assettracking.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val rooms: List<com.example.assettracking.domain.model.LocationSummary> = emptyList(),
    val isLoading: Boolean = true,
    val message: UiMessage? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeRoomsUseCase: ObserveRoomsUseCase,
    private val assignAssetToRoomWithConditionUseCase: AssignAssetToRoomWithConditionUseCase,
    private val findAssetByIdUseCase: FindAssetByIdUseCase,
    private val createMovementUseCase: CreateMovementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeRooms()
    }

    private fun observeRooms() {
        observeRoomsUseCase()
            .onEach { rooms ->
                _uiState.update { it.copy(rooms = rooms, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun assignAssetToRoom(assetCode: String, roomId: Long, condition: String) {
        viewModelScope.launch {
            val assetId = assetCode.toLongOrNull()
            if (assetId != null) {
                // First, find the asset to get its current location
                val asset = findAssetByIdUseCase(assetId)
                if (asset == null) {
                    _uiState.update { it.copy(message = UiMessage("Asset not found")) }
                    return@launch
                }

                // Assign asset to room with condition
                val result = assignAssetToRoomWithConditionUseCase(assetId, roomId, condition)
                result.onFailure { error ->
                    _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to move asset")) }
                    return@launch
                }

                // Log movement for audit trail
                try {
                    createMovementUseCase(asset.id, asset.currentRoomId, roomId)
                } catch (error: Exception) {
                    _uiState.update {
                        it.copy(message = UiMessage(error.message ?: "Asset moved but movement not logged"))
                    }
                }
            } else {
                _uiState.update { it.copy(message = UiMessage("Invalid asset ID")) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
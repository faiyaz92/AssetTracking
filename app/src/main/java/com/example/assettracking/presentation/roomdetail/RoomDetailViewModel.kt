package com.example.assettracking.presentation.roomdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.domain.usecase.CreateMovementUseCase
import com.example.assettracking.domain.usecase.DetachAssetFromRoomUseCase
import com.example.assettracking.domain.usecase.FindAssetByIdUseCase
import com.example.assettracking.domain.usecase.ObserveRoomDetailUseCase
import com.example.assettracking.domain.usecase.UpdateCurrentRoomUseCase
import com.example.assettracking.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ROOM_ID_KEY = "roomId"

@HiltViewModel
class RoomDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeRoomDetailUseCase: ObserveRoomDetailUseCase,
    private val detachAssetFromRoomUseCase: DetachAssetFromRoomUseCase,
    private val findAssetByIdUseCase: FindAssetByIdUseCase,
    private val updateCurrentRoomUseCase: UpdateCurrentRoomUseCase,
    private val createMovementUseCase: CreateMovementUseCase
) : ViewModel() {

    private val roomId: Long = checkNotNull(savedStateHandle[ROOM_ID_KEY])

    private val _uiState = MutableStateFlow(RoomDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeRoomDetails()
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
            // First, find the asset to get its current room
            val asset = findAssetByIdUseCase(assetId)
            if (asset == null) {
                _uiState.update { it.copy(message = UiMessage("Asset not found")) }
                return@launch
            }

            // Update current room
            val updateResult = updateCurrentRoomUseCase(asset.id, roomId)
            if (updateResult.isFailure) {
                _uiState.update { it.copy(message = UiMessage("Failed to update asset location")) }
                return@launch
            }

            // Log movement for audit trail
            try {
                createMovementUseCase(asset.id, asset.currentRoomId, roomId)
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

    private fun observeRoomDetails() {
        observeRoomDetailUseCase(roomId)
            .onEach { detail ->
                _uiState.update { state ->
                    state.copy(
                        roomDetail = detail,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}

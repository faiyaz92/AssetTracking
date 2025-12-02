package com.example.assettracking.presentation.tabs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.domain.usecase.AssignAssetToRoomWithConditionUseCase
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
    val rooms: List<com.example.assettracking.domain.model.RoomSummary> = emptyList(),
    val isLoading: Boolean = true,
    val message: UiMessage? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeRoomsUseCase: ObserveRoomsUseCase,
    private val assignAssetToRoomWithConditionUseCase: AssignAssetToRoomWithConditionUseCase
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
            val result = assignAssetToRoomWithConditionUseCase(assetCode, roomId, condition)
            result.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to move asset")) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
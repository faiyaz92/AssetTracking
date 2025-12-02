package com.example.assettracking.presentation.tabs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.presentation.common.UiMessage
import com.example.assettracking.presentation.tabs.model.RoomListEvent
import com.example.assettracking.presentation.tabs.model.RoomListUiState
import com.example.assettracking.domain.usecase.CreateRoomUseCase
import com.example.assettracking.domain.usecase.DeleteRoomUseCase
import com.example.assettracking.domain.usecase.ObserveRoomsUseCase
import com.example.assettracking.domain.usecase.UpdateRoomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RoomListViewModel @Inject constructor(
    private val observeRoomsUseCase: ObserveRoomsUseCase,
    private val createRoomUseCase: CreateRoomUseCase,
    private val updateRoomUseCase: UpdateRoomUseCase,
    private val deleteRoomUseCase: DeleteRoomUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeRooms()
    }

    fun onEvent(event: RoomListEvent) {
        when (event) {
            is RoomListEvent.CreateRoom -> createRoom(event.name, event.description)
            is RoomListEvent.UpdateRoom -> updateRoom(event.id, event.name, event.description)
            is RoomListEvent.DeleteRoom -> deleteRoom(event.id)
            RoomListEvent.ClearMessage -> _uiState.update { it.copy(message = null) }
        }
    }

    private fun observeRooms() {
        observeRoomsUseCase()
            .onEach { rooms ->
                _uiState.update { state ->
                    state.copy(
                        rooms = rooms,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun createRoom(name: String, description: String?) {
        viewModelScope.launch {
            val result = createRoomUseCase(name, description)
            result.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to create room")) }
            }
        }
    }

    private fun updateRoom(id: Long, name: String, description: String?) {
        viewModelScope.launch {
            val result = updateRoomUseCase(id, name, description)
            result.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to update room")) }
            }
        }
    }

    private fun deleteRoom(id: Long) {
        viewModelScope.launch {
            val result = deleteRoomUseCase(id)
            result.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to delete room")) }
            }
        }
    }
}

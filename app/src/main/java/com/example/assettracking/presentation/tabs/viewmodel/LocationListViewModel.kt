package com.example.assettracking.presentation.tabs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.presentation.common.UiMessage
import com.example.assettracking.presentation.tabs.model.LocationListEvent
import com.example.assettracking.presentation.tabs.model.LocationListUiState
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
class LocationListViewModel @Inject constructor(
    private val observeRoomsUseCase: ObserveRoomsUseCase,
    private val createRoomUseCase: CreateRoomUseCase,
    private val updateRoomUseCase: UpdateRoomUseCase,
    private val deleteRoomUseCase: DeleteRoomUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeRooms()
    }

    fun onEvent(event: LocationListEvent) {
        when (event) {
            is LocationListEvent.CreateLocation -> createRoom(event.name, event.description)
            is LocationListEvent.UpdateLocation -> updateRoom(event.id, event.name, event.description)
            is LocationListEvent.DeleteLocation -> deleteRoom(event.id)
            LocationListEvent.ClearMessage -> _uiState.update { it.copy(message = null) }
        }
    }

    private fun observeRooms() {
        observeRoomsUseCase()
            .onEach { locations ->
                _uiState.update { state ->
                    state.copy(
                        locations = locations,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun createRoom(name: String, description: String?, parentId: Long? = null) {
        viewModelScope.launch {
            val result = createRoomUseCase(name, description, parentId)
            result.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to create location")) }
            }
        }
    }

    private fun updateRoom(id: Long, name: String, description: String?) {
        viewModelScope.launch {
            val result = updateRoomUseCase(id, name, description)
            result.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to update location")) }
            }
        }
    }

    private fun deleteRoom(id: Long) {
        viewModelScope.launch {
            val result = deleteRoomUseCase(id)
            result.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to delete location")) }
            }
        }
    }
}

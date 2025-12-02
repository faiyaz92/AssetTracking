package com.example.assettracking.presentation.tabs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.presentation.common.UiMessage
import com.example.assettracking.presentation.tabs.model.AssetListEvent
import com.example.assettracking.presentation.tabs.model.AssetListUiState
import com.example.assettracking.domain.usecase.CreateAssetUseCase
import com.example.assettracking.domain.usecase.DeleteAssetUseCase
import com.example.assettracking.domain.usecase.ObserveAssetsUseCase
import com.example.assettracking.domain.usecase.ObserveRoomsUseCase
import com.example.assettracking.domain.usecase.UpdateAssetUseCase
import com.example.assettracking.domain.usecase.UpdateCurrentRoomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AssetListViewModel @Inject constructor(
    private val observeAssetsUseCase: ObserveAssetsUseCase,
    private val observeRoomsUseCase: ObserveRoomsUseCase,
    private val createAssetUseCase: CreateAssetUseCase,
    private val updateAssetUseCase: UpdateAssetUseCase,
    private val deleteAssetUseCase: DeleteAssetUseCase,
    private val updateCurrentRoomUseCase: UpdateCurrentRoomUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeAssets()
        observeRooms()
    }

    fun onEvent(event: AssetListEvent) {
        when (event) {
            is AssetListEvent.CreateAsset -> createAsset(event.code, event.name, event.details, event.condition, event.baseRoomId)
            is AssetListEvent.UpdateAsset -> updateAsset(event.id, event.code, event.name, event.details, event.condition)
            is AssetListEvent.DeleteAsset -> deleteAsset(event.id)
            is AssetListEvent.UpdateSearch -> updateSearch(event.query)
            AssetListEvent.ClearMessage -> _uiState.update { it.copy(message = null) }
        }
    }

    private fun observeAssets() {
        observeAssetsUseCase()
            .onEach { assets ->
                val currentQuery = _uiState.value.searchQuery
                _uiState.update { state ->
                    val filtered = if (currentQuery.isBlank()) assets else assets.filter { asset ->
                        asset.code.contains(currentQuery, ignoreCase = true) ||
                            asset.name.contains(currentQuery, ignoreCase = true)
                    }
                    state.copy(
                        assets = assets,
                        filteredAssets = filtered,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun createAsset(code: String, name: String, details: String?, condition: String?, baseRoomId: Long?) {
        viewModelScope.launch {
            val result = createAssetUseCase(code, name, details, condition, baseRoomId)
            result.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to create asset")) }
            }
        }
    }

    private fun updateAsset(assetId: Long, code: String, name: String, details: String?, condition: String?) {
        viewModelScope.launch {
            val result = updateAssetUseCase(assetId, code, name, details, condition)
            result.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to update asset")) }
            }
        }
    }

    private fun deleteAsset(assetId: Long) {
        viewModelScope.launch {
            val result = deleteAssetUseCase(assetId)
            result.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to delete asset")) }
            }
        }
    }

    private fun observeRooms() {
        observeRoomsUseCase()
            .onEach { rooms ->
                _uiState.update { state ->
                    state.copy(rooms = rooms)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updateSearch(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.assets
            } else {
                state.assets.filter { asset ->
                    asset.code.contains(query, ignoreCase = true) ||
                        asset.name.contains(query, ignoreCase = true)
                }
            }
            state.copy(searchQuery = query, filteredAssets = filtered)
        }
    }
}

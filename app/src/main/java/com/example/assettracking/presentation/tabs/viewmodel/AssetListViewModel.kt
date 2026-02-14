package com.example.assettracking.presentation.tabs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.presentation.common.UiMessage
import com.example.assettracking.presentation.tabs.model.AssetListEvent
import com.example.assettracking.presentation.tabs.model.AssetListUiState
import com.example.assettracking.presentation.tabs.model.GroupingMode
import com.example.assettracking.presentation.tabs.model.SubGroup
import com.example.assettracking.presentation.tabs.model.GroupedItem
import com.example.assettracking.domain.model.LocationSummary
import com.example.assettracking.domain.usecase.CreateAssetUseCase
import com.example.assettracking.domain.usecase.DeleteAssetUseCase
import com.example.assettracking.domain.usecase.ObserveAssetsUseCase
import com.example.assettracking.domain.usecase.ObserveAllLocationsUseCase
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
    private val observeAllLocationsUseCase: ObserveAllLocationsUseCase,
    private val createAssetUseCase: CreateAssetUseCase,
    private val updateAssetUseCase: UpdateAssetUseCase,
    private val deleteAssetUseCase: DeleteAssetUseCase,
    private val updateCurrentRoomUseCase: UpdateCurrentRoomUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeAssets()
        observeAllLocations()
    }

    fun onEvent(event: AssetListEvent) {
        when (event) {
            is AssetListEvent.CreateAsset -> createAsset(event.name, event.details, event.condition, event.baseRoomId)
            is AssetListEvent.UpdateAsset -> updateAsset(event.id, event.name, event.details, event.condition, event.baseRoomId)
            is AssetListEvent.DeleteAsset -> deleteAsset(event.id)
            is AssetListEvent.UpdateSearch -> updateSearch(event.query)
            AssetListEvent.ClearMessage -> _uiState.update { it.copy(message = null) }

            // Grouping and filtering events
            AssetListEvent.ToggleGroupByCurrentLocation -> toggleGroupByCurrentLocation()
            AssetListEvent.ToggleGroupByBaseLocation -> toggleGroupByBaseLocation()
            is AssetListEvent.FilterByCurrentLocation -> filterByCurrentLocation(event.roomId)
            is AssetListEvent.FilterByBaseLocation -> filterByBaseLocation(event.roomId)
            is AssetListEvent.FilterByStatus -> filterByStatus(event.status)
            is AssetListEvent.ChangeGroupingMode -> changeGroupingMode(event.mode)
            AssetListEvent.ClearAllFilters -> clearAllFilters()
        }
    }

    private fun observeAssets() {
        observeAssetsUseCase()
            .onEach { assets ->
                _uiState.update { state ->
                    val updatedState = state.copy(assets = assets, isLoading = false)
                    updateDisplayData(updatedState)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun createAsset(name: String, details: String?, condition: String?, baseRoomId: Long?) {
        viewModelScope.launch {
            val result = createAssetUseCase(name, details, condition, baseRoomId)
            result.onSuccess {
                _uiState.update { it.copy(message = UiMessage("Asset created successfully")) }
            }.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to create asset")) }
            }
        }
    }

    private fun updateAsset(assetId: Long, name: String, details: String?, condition: String?, baseRoomId: Long?) {
        viewModelScope.launch {
            val result = updateAssetUseCase(assetId, name, details, condition, baseRoomId)
            result.onSuccess {
                _uiState.update { it.copy(message = UiMessage("Asset updated successfully")) }
            }.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to update asset")) }
            }
        }
    }

    private fun deleteAsset(assetId: Long) {
        viewModelScope.launch {
            val result = deleteAssetUseCase(assetId)
            result.onSuccess {
                _uiState.update { it.copy(message = UiMessage("Asset deleted successfully")) }
            }.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to delete asset")) }
            }
        }
    }

    private fun observeAllLocations() {
        observeAllLocationsUseCase()
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
                    asset.id.toString().padStart(6, '0').contains(query, ignoreCase = true) ||
                        asset.name?.contains(query, ignoreCase = true) == true
                }
            }
            val updatedState = state.copy(searchQuery = query, filteredAssets = filtered)
            updateDisplayData(updatedState)
        }
    }

    private fun toggleGroupByCurrentLocation() {
        _uiState.update { state ->
            val updatedState = state.copy(
                isGroupedByCurrentLocation = !state.isGroupedByCurrentLocation,
                isGroupedByBaseLocation = false // Only one grouping at a time
            )
            updateDisplayData(updatedState)
        }
    }

    private fun toggleGroupByBaseLocation() {
        _uiState.update { state ->
            val updatedState = state.copy(
                isGroupedByBaseLocation = !state.isGroupedByBaseLocation,
                isGroupedByCurrentLocation = false // Only one grouping at a time
            )
            updateDisplayData(updatedState)
        }
    }

    private fun filterByCurrentLocation(roomId: Long?) {
        _uiState.update { state ->
            val updatedState = state.copy(currentLocationFilter = roomId)
            updateDisplayData(updatedState)
        }
    }

    private fun filterByBaseLocation(roomId: Long?) {
        _uiState.update { state ->
            val updatedState = state.copy(baseLocationFilter = roomId)
            updateDisplayData(updatedState)
        }
    }

    private fun filterByStatus(status: String?) {
        _uiState.update { state ->
            val updatedState = state.copy(statusFilter = status)
            updateDisplayData(updatedState)
        }
    }

    private fun clearAllFilters() {
        _uiState.update { state ->
            val updatedState = state.copy(
                isGroupedByCurrentLocation = false,
                isGroupedByBaseLocation = false,
                currentLocationFilter = null,
                baseLocationFilter = null,
                statusFilter = null
            )
            updateDisplayData(updatedState)
        }
    }

    private fun changeGroupingMode(mode: GroupingMode) {
        _uiState.update { state ->
            val updatedState = state.copy(groupingMode = mode)
            updateDisplayData(updatedState)
        }
    }

    private fun updateDisplayData(state: AssetListUiState): AssetListUiState {
        // Apply filters first, then search within filtered results
        var filteredAssets = state.assets

        // Apply location filters first
        if (state.currentLocationFilter != null) {
            filteredAssets = filteredAssets.filter { it.currentRoomId == state.currentLocationFilter }
        }
        if (state.baseLocationFilter != null) {
            filteredAssets = filteredAssets.filter { it.baseRoomId == state.baseLocationFilter }
        }
        if (state.statusFilter != null) {
            filteredAssets = filteredAssets.filter { it.status == state.statusFilter }
        }

        // Apply search filter to filtered results
        if (state.searchQuery.isNotBlank()) {
            filteredAssets = filteredAssets.filter { asset ->
                asset.id.toString().padStart(6, '0').contains(state.searchQuery, ignoreCase = true) ||
                    asset.name?.contains(state.searchQuery, ignoreCase = true) == true
            }
        }

        // Apply grouping
        val groupedAssets = if (state.isGroupedByCurrentLocation) {
            filteredAssets.groupBy { asset ->
                asset.currentRoomName ?: "Unassigned"
            }
        } else if (state.isGroupedByBaseLocation) {
            filteredAssets.groupBy { asset ->
                asset.baseRoomName ?: "No Base Location"
            }
        } else {
            emptyMap()
        }

        // Compute groupedData
        val groupedData = when (state.groupingMode) {
            GroupingMode.NONE -> emptyList()
            GroupingMode.BY_BASE_LOCATION -> {
                val groups = filteredAssets.groupBy { it.baseRoomId }
                groups.map { (baseId, assets) ->
                    val location = baseId?.let { id -> state.rooms.find { it.id == id } }
                        ?: LocationSummary(
                            id = -1L,
                            name = "Unassigned",
                            description = null,
                            assetCount = 0,
                            parentId = null,
                            hasChildren = false,
                            locationCode = "UNASSIGNED"
                        )
                    val subGroups = assets.groupBy { it.currentRoomId }.map { (currentId, subAssets) ->
                        val subLocation = currentId?.let { id -> state.rooms.find { it.id == id } }
                            ?: LocationSummary(
                                id = -2L,
                                name = "Not Assigned",
                                description = null,
                                assetCount = 0,
                                parentId = null,
                                hasChildren = false,
                                locationCode = "NOT_ASSIGNED"
                            )
                        SubGroup(subLocation, subAssets)
                    }
                    GroupedItem(location, assets, subGroups)
                }
            }
            GroupingMode.BY_CURRENT_LOCATION -> {
                // Group 1: Assets with a current location (normal grouping)
                val currentLocationGroups = filteredAssets
                    .filter { it.currentRoomId != null }
                    .groupBy { it.currentRoomId }
                    .map { (currentId, assets) ->
                        val location = state.rooms.find { it.id == currentId }
                            ?: LocationSummary(
                                id = currentId ?: -10L,
                                name = "Unknown Location",
                                description = null,
                                assetCount = 0,
                                parentId = null,
                                hasChildren = false,
                                locationCode = "UNKNOWN"
                            )

                        val subGroups = assets.groupBy { it.baseRoomId }.map { (baseId, subAssets) ->
                            val subLocation = baseId?.let { id -> state.rooms.find { it.id == id } }
                                ?: LocationSummary(
                                    id = -1L,
                                    name = "Unassigned",
                                    description = null,
                                    assetCount = 0,
                                    parentId = null,
                                    hasChildren = false,
                                    locationCode = "UNASSIGNED"
                                )
                            SubGroup(subLocation, subAssets)
                        }

                        GroupedItem(location, assets, subGroups)
                    }

                // Group 2: Missing — base set, but current location is null
                val missingAssets = filteredAssets.filter { it.currentRoomId == null && it.baseRoomId != null }
                val missingGroup = if (missingAssets.isNotEmpty()) {
                    val missingSubGroups = missingAssets.groupBy { it.baseRoomId }.map { (baseId, subAssets) ->
                        val subLocation = baseId?.let { id -> state.rooms.find { it.id == id } }
                            ?: LocationSummary(
                                id = baseId ?: -20L,
                                name = "Unknown Base",
                                description = null,
                                assetCount = 0,
                                parentId = null,
                                hasChildren = false,
                                locationCode = "UNKNOWN_BASE"
                            )
                        SubGroup(subLocation, subAssets)
                    }

                    GroupedItem(
                        location = LocationSummary(
                            id = -2L,
                            name = "Missing",
                            description = null,
                            assetCount = 0,
                            parentId = null,
                            hasChildren = false,
                            locationCode = "MISSING"
                        ),
                        assets = missingAssets,
                        subGroups = missingSubGroups
                    )
                } else {
                    null
                }

                // Group 3: Unassigned — base location is null (no further subgrouping)
                val unassignedAssets = filteredAssets.filter { it.baseRoomId == null }
                val unassignedGroup = if (unassignedAssets.isNotEmpty()) {
                    GroupedItem(
                        location = LocationSummary(
                            id = -3L,
                            name = "Unassigned",
                            description = null,
                            assetCount = 0,
                            parentId = null,
                            hasChildren = false,
                            locationCode = "UNASSIGNED"
                        ),
                        assets = unassignedAssets,
                        subGroups = listOf(SubGroup(null, unassignedAssets))
                    )
                } else {
                    null
                }

                buildList {
                    addAll(currentLocationGroups)
                    missingGroup?.let { add(it) }
                    unassignedGroup?.let { add(it) }
                }
            }
        }

        val nonEmptyGroupedData = groupedData.filter { it.assets.isNotEmpty() }

        return state.copy(
            filteredAssets = filteredAssets,
            groupedAssets = groupedAssets,
            groupedData = nonEmptyGroupedData
        )
    }
}

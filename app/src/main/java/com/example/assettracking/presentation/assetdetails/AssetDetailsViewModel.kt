package com.example.assettracking.presentation.assetdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.domain.usecase.FindAssetByIdUseCase
import com.example.assettracking.domain.usecase.ObserveMovementsForAssetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetDetailsViewModel @Inject constructor(
    private val findAssetByIdUseCase: FindAssetByIdUseCase,
    private val observeMovementsForAssetUseCase: ObserveMovementsForAssetUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val assetId: Long = checkNotNull(savedStateHandle["assetId"])

    private val _uiState = MutableStateFlow(AssetDetailsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAsset()
        observeMovements()
    }

    private fun loadAsset() {
        viewModelScope.launch {
            val asset = findAssetByIdUseCase(assetId)
            _uiState.update { it.copy(asset = asset, isLoading = false) }
        }
    }

    private fun observeMovements() {
        observeMovementsForAssetUseCase(assetId)
            .onEach { movements ->
                _uiState.update { it.copy(movements = movements.sortedByDescending { it.timestamp }) }
            }
            .launchIn(viewModelScope)
    }
}
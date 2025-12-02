package com.example.assettracking.presentation.tabs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.domain.usecase.ObserveMovementsUseCase
import com.example.assettracking.presentation.tabs.model.AuditTrailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@HiltViewModel
class AuditTrailViewModel @Inject constructor(
    private val observeMovementsUseCase: ObserveMovementsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditTrailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeMovements()
    }

    private fun observeMovements() {
        observeMovementsUseCase()
            .onEach { movements ->
                _uiState.update { state ->
                    state.copy(
                        movements = movements.sortedByDescending { it.timestamp },
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
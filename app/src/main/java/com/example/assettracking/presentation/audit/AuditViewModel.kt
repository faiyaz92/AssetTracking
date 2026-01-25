package com.example.assettracking.presentation.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.domain.model.AuditType
import com.example.assettracking.domain.usecase.CreateAuditUseCase
import com.example.assettracking.domain.usecase.FinishAuditUseCase
import com.example.assettracking.domain.usecase.ObserveAllLocationsUseCase
import com.example.assettracking.domain.usecase.ObserveAuditsUseCase
import com.example.assettracking.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuditViewModel @Inject constructor(
    private val observeAuditsUseCase: ObserveAuditsUseCase,
    private val observeAllLocationsUseCase: ObserveAllLocationsUseCase,
    private val createAuditUseCase: CreateAuditUseCase,
    private val finishAuditUseCase: FinishAuditUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeAudits()
        observeLocations()
    }

    private fun observeAudits() {
        observeAuditsUseCase()
            .onEach { audits ->
                _uiState.update { it.copy(audits = audits, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeLocations() {
        observeAllLocationsUseCase()
            .onEach { locations ->
                _uiState.update { it.copy(locations = locations) }
            }
            .launchIn(viewModelScope)
    }

    fun createAudit(name: String, locationId: Long?, type: AuditType, includeChildren: Boolean) {
        viewModelScope.launch {
            val targetLocationId = locationId
            if (targetLocationId == null) {
                _uiState.update { it.copy(message = UiMessage("Select a location")) }
                return@launch
            }
            val result = createAuditUseCase(name, targetLocationId, type, includeChildren)
            result.onSuccess {
                _uiState.update { it.copy(message = UiMessage("Audit created")) }
            }.onFailure { error ->
                _uiState.update { it.copy(message = UiMessage(error.message ?: "Failed to create audit")) }
            }
        }
    }

    fun finishAudit(auditId: Long) {
        viewModelScope.launch {
            finishAuditUseCase(auditId)
                .onSuccess { _uiState.update { it.copy(message = UiMessage("Audit marked finished")) } }
                .onFailure { error -> _uiState.update { it.copy(message = UiMessage(error.message ?: "Unable to finish audit")) } }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

package com.example.assettracking.presentation.audit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.domain.model.AuditType
import com.example.assettracking.domain.usecase.FinishAuditUseCase
import com.example.assettracking.domain.usecase.GetAuditUseCase
import com.example.assettracking.domain.usecase.ObserveAllLocationsUseCase
import com.example.assettracking.domain.usecase.ObserveAssetsUseCase
import com.example.assettracking.domain.usecase.ObserveMovementsUseCase
import com.example.assettracking.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val AUDIT_ID_KEY = "auditId"

@HiltViewModel
class AuditDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAuditUseCase: GetAuditUseCase,
    private val finishAuditUseCase: FinishAuditUseCase,
    private val observeMovementsUseCase: ObserveMovementsUseCase,
    private val observeAssetsUseCase: ObserveAssetsUseCase,
    private val observeAllLocationsUseCase: ObserveAllLocationsUseCase
) : ViewModel() {

    private val auditId: Long = checkNotNull(savedStateHandle[AUDIT_ID_KEY])

    private val _uiState = MutableStateFlow(AuditDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAudit()
        observeSupportingData()
    }

    private fun loadAudit() {
        viewModelScope.launch {
            val audit = getAuditUseCase(auditId)
            _uiState.update { it.copy(audit = audit, isLoading = audit == null) }
        }
    }

    private fun observeSupportingData() {
        val movementsFlow = observeMovementsUseCase()
        val assetsFlow = observeAssetsUseCase()
        val locationsFlow = observeAllLocationsUseCase()

        combine(movementsFlow, assetsFlow, locationsFlow, _uiState) { movements, assets, locations, state ->
            val audit = state.audit ?: return@combine state
            val scopeIds = buildScopeIds(audit, locations)
            val assetBaseMap = assets.associate { it.id to it.baseRoomId }
            val filtered = filterMovements(audit, movements, scopeIds, assetBaseMap)
            state.copy(
                movements = filtered,
                locations = locations,
                assetBases = assetBaseMap,
                isLoading = false
            )
        }.onEach { updated ->
            _uiState.value = updated
        }.launchIn(viewModelScope)
    }

    private fun buildScopeIds(audit: com.example.assettracking.domain.model.AuditRecord, locations: List<com.example.assettracking.domain.model.LocationSummary>): Set<Long> {
        if (!audit.includeChildren) return setOf(audit.locationId)
        val lookup = locations.associateBy { it.id }
        val children = mutableSetOf<Long>()
        val queue = ArrayDeque<Long>()
        queue.add(audit.locationId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            children.add(current)
            locations.filter { it.parentId == current }.forEach { child ->
                queue.add(child.id)
            }
        }
        return children
    }

    private fun filterMovements(
        audit: com.example.assettracking.domain.model.AuditRecord,
        movements: List<com.example.assettracking.domain.model.AssetMovement>,
        scopeIds: Set<Long>,
        assetBaseMap: Map<Long, Long?>
    ): List<com.example.assettracking.domain.model.AssetMovement> {
        val start = audit.createdAt
        val end = audit.finishedAt ?: System.currentTimeMillis()
        return movements.filter { movement ->
            movement.timestamp in start..end &&
                when (audit.type) {
                    AuditType.MINI -> miniFilter(movement, scopeIds)
                    AuditType.FULL -> fullFilter(movement, scopeIds, assetBaseMap)
                }
        }
    }

    private fun miniFilter(movement: com.example.assettracking.domain.model.AssetMovement, scopeIds: Set<Long>): Boolean {
        return (movement.fromRoomId != null && scopeIds.contains(movement.fromRoomId)) ||
            scopeIds.contains(movement.toRoomId)
    }

    private fun fullFilter(
        movement: com.example.assettracking.domain.model.AssetMovement,
        scopeIds: Set<Long>,
        assetBaseMap: Map<Long, Long?>
    ): Boolean {
        if (miniFilter(movement, scopeIds)) return true
        val base = assetBaseMap[movement.assetId]
        return base != null && scopeIds.contains(base)
    }

    fun finishAudit() {
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

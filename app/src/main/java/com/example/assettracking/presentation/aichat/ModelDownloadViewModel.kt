package com.example.assettracking.presentation.aichat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val modelManager = LocalModelManager(application)

    private val _uiState = MutableStateFlow(
        ModelDownloadUiState(
            statuses = LocalModel.values().associateWith { model ->
                ModelStatus(
                    isDownloaded = modelManager.isDownloaded(model),
                    progress = if (modelManager.isDownloaded(model)) 100 else 0,
                    filePath = modelManager.fileFor(model).takeIf { it.exists() }?.absolutePath
                )
            }
        )
    )
    val uiState: StateFlow<ModelDownloadUiState> = _uiState

    fun refresh() {
        _uiState.update { state ->
            state.copy(
                statuses = state.statuses.mapValues { (model, _) ->
                    ModelStatus(
                        isDownloaded = modelManager.isDownloaded(model),
                        progress = if (modelManager.isDownloaded(model)) 100 else 0,
                        filePath = modelManager.fileFor(model).takeIf { it.exists() }?.absolutePath
                    )
                }
            )
        }
    }

    fun download(model: LocalModel) {
        // Avoid parallel downloads of the same model
        val current = _uiState.value.statuses[model]
        if (current?.isDownloaded == true || (current?.error == null && current?.progress in 1..99)) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    statuses = state.statuses + (model to ModelStatus(isDownloaded = false, progress = 0, error = null))
                )
            }

            try {
                modelManager.downloadModel(model) { progress ->
                    _uiState.update { state ->
                        state.copy(
                            statuses = state.statuses + (model to ModelStatus(isDownloaded = false, progress = progress, error = null))
                        )
                    }
                }
                val filePath = modelManager.fileFor(model).absolutePath
                _uiState.update { state ->
                    state.copy(
                        statuses = state.statuses + (model to ModelStatus(isDownloaded = true, progress = 100, error = null, filePath = filePath))
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        statuses = state.statuses + (model to ModelStatus(isDownloaded = false, progress = 0, error = e.message))
                    )
                }
            }
        }
    }
}

data class ModelDownloadUiState(
    val statuses: Map<LocalModel, ModelStatus> = emptyMap()
)

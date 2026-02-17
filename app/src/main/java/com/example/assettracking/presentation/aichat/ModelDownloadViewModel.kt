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
                    val file = modelManager.fileFor(model)
                    val info = modelManager.infoFor(model)
                    ModelStatus(
                        isDownloaded = file.exists(),
                        progress = if (file.exists()) 100 else 0,
                        filePath = file.takeIf { it.exists() }?.absolutePath,
                        downloadedBytes = if (file.exists()) file.length() else 0,
                        totalBytes = info.sizeBytes
                    )
                }
            )
        }
    }

    fun download(model: LocalModel) {
        // Avoid parallel downloads of the same model
        val current = _uiState.value.statuses[model]
        if (current?.isDownloaded == true || (current?.error == null && current?.progress in 1..99)) return

        val startTime = System.currentTimeMillis()
        val info = modelManager.infoFor(model)

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    statuses = state.statuses + (model to ModelStatus(
                        isDownloaded = false,
                        progress = 0,
                        error = null,
                        downloadedBytes = 0,
                        totalBytes = info.sizeBytes
                    ))
                )
            }

            try {
                modelManager.downloadModel(model) { progress, downloadedBytes, totalBytes ->
                    val elapsed = System.currentTimeMillis() - startTime
                    val speed = if (elapsed > 0) (downloadedBytes * 1000 / elapsed) else 0
                    val speedText = formatSpeed(speed)

                    _uiState.update { state ->
                        state.copy(
                            statuses = state.statuses + (model to ModelStatus(
                                isDownloaded = false,
                                progress = progress,
                                error = null,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                downloadSpeed = speedText
                            ))
                        )
                    }
                }
                val filePath = modelManager.fileFor(model).absolutePath
                _uiState.update { state ->
                    state.copy(
                        statuses = state.statuses + (model to ModelStatus(
                            isDownloaded = true,
                            progress = 100,
                            error = null,
                            filePath = filePath,
                            downloadedBytes = info.sizeBytes,
                            totalBytes = info.sizeBytes
                        ))
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        statuses = state.statuses + (model to ModelStatus(
                            isDownloaded = false,
                            progress = 0,
                            error = e.message,
                            downloadedBytes = 0,
                            totalBytes = info.sizeBytes
                        ))
                    )
                }
            }
        }
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1_000_000_000 -> "%.1f GB/s".format(bytesPerSecond / 1_000_000_000.0)
            bytesPerSecond >= 1_000_000 -> "%.1f MB/s".format(bytesPerSecond / 1_000_000.0)
            bytesPerSecond >= 1_000 -> "%.1f KB/s".format(bytesPerSecond / 1_000.0)
            else -> "$bytesPerSecond B/s"
        }
    }
}

data class ModelDownloadUiState(
    val statuses: Map<LocalModel, ModelStatus> = emptyMap()
)

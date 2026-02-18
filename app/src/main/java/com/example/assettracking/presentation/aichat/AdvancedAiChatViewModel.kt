package com.example.assettracking.presentation.aichat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assettracking.data.local.dao.ChatMessageDao
import com.example.assettracking.data.local.entity.ChatMessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdvancedChatMessage(
    val id: String,
    val htmlContent: String,  // HTML response
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class AdvancedAiChatState(
    val messages: List<AdvancedChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedModel: AdvancedModel = AdvancedModel.Gemma
)

enum class AdvancedModel { Gemma, TinyFB, DeepSeekR1 }

@HiltViewModel
class AdvancedAiChatViewModel @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedAiChatState())
    val uiState: StateFlow<AdvancedAiChatState> = _uiState

    private val modelManager = LocalModelManager(application)

    init {
        viewModelScope.launch {
            // Load previous messages if needed, but for now, start fresh
        }
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.trim().isBlank()) return

        val messageId = System.currentTimeMillis().toString()

        viewModelScope.launch {
            // Insert user message
            val userMsg = AdvancedChatMessage(
                id = messageId,
                htmlContent = userMessage,
                isUser = true
            )
            // For simplicity, not saving to DB, but can add if needed

            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val localModel = when (_uiState.value.selectedModel) {
                    AdvancedModel.Gemma -> LocalModel.Gemma
                    AdvancedModel.TinyFB -> LocalModel.TinyLlama
                    AdvancedModel.DeepSeekR1 -> LocalModel.DeepSeekR1
                }

                val modelFile = modelManager.fileFor(localModel)
                if (!modelFile.exists()) {
                    _uiState.update {
                        it.copy(
                            messages = it.messages + userMsg,
                            isLoading = false,
                            error = "Selected model not downloaded. Please download it first."
                        )
                    }
                    return@launch
                }

                // Check if model file is complete by comparing size
                val expectedSize = modelManager.infoFor(localModel).sizeBytes
                val actualSize = modelFile.length()
                if (actualSize < expectedSize * 0.9) { // Allow 10% tolerance for download issues
                    _uiState.update {
                        it.copy(
                            messages = it.messages + userMsg,
                            isLoading = false,
                            error = "Model file appears incomplete. Expected: ${expectedSize / (1024*1024)}MB, Got: ${actualSize / (1024*1024)}MB. Please re-download."
                        )
                    }
                    return@launch
                }

                val engine = AdvancedAiEngine(application, modelFile.absolutePath)
                val htmlResponse = engine.generateResponse(userMessage)
                engine.close()

                val aiMsg = AdvancedChatMessage(
                    id = "${messageId}_response",
                    htmlContent = htmlResponse,
                    isUser = false
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + userMsg + aiMsg,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        messages = it.messages + userMsg,
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun setModel(model: AdvancedModel) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
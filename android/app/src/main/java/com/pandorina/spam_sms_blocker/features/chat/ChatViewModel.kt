package com.pandorina.spam_sms_blocker.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pandorina.spam_sms_blocker.data.entity.MessageEntity
import com.pandorina.spam_sms_blocker.domain.repository.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val smsRepository: SmsRepository
) : ViewModel() {

    data class ChatUiState(
        val messages: List<MessageEntity> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
        val currentThreadId: Long? = null,
        val recipientAddress: String = ""
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun loadMessages(threadId: Long) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    error = null, 
                    currentThreadId = threadId
                ) 
            }

            try {
                smsRepository.getMessagesByThreadId(threadId)
                    .catch { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to load messages: ${error.message}"
                            )
                        }
                    }
                    .collect { messages ->
                        val recipientAddress = messages.firstOrNull()?.address ?: "Unknown"
                        _uiState.update {
                            it.copy(
                                messages = messages,
                                isLoading = false,
                                error = null,
                                recipientAddress = recipientAddress
                            )
                        }
                        
                        // Mark all messages in this thread as read
                        try {
                            smsRepository.markThreadMessagesAsRead(threadId)
                        } catch (e: Exception) {
                            // If marking as read fails, don't break the flow
                            // Just continue with loading messages
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load messages: ${e.message}"
                    )
                }
            }
        }
    }

    fun refreshMessages() {
        _uiState.value.currentThreadId?.let { threadId ->
            loadMessages(threadId)
        }
    }
} 
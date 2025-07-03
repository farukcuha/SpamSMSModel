package com.pandorina.spam_sms_blocker.features.threads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pandorina.spam_sms_blocker.data.dao.MessageDao
import com.pandorina.spam_sms_blocker.domain.repository.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ThreadFilter {
    ALL, NORMAL, SPAM
}

@HiltViewModel
class ThreadsViewModel @Inject constructor(
    private val smsRepository: SmsRepository
) : ViewModel() {

    data class ThreadsUiState(
        val threads: List<MessageDao.ThreadData> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
        val currentFilter: ThreadFilter = ThreadFilter.ALL,
        val allThreadsCount: Int = 0,
        val normalThreadsCount: Int = 0,
        val spamThreadsCount: Int = 0,
        // Selection state
        val isSelectionMode: Boolean = false,
        val selectedThreadIds: Set<Long> = emptySet(),
        val isDeleting: Boolean = false,
        val showDeleteConfirmation: Boolean = false
    )

    private val _uiState = MutableStateFlow(ThreadsUiState())
    val uiState: StateFlow<ThreadsUiState> = _uiState.asStateFlow()

    // Shared flow for all threads - cached and shared across multiple collectors
    private val allThreadsFlow = smsRepository.getAllThreads()
        .distinctUntilChanged()
        .shareIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    // Derived flows from the main flow for better performance
    private val threadCountsFlow = allThreadsFlow
        .map { threads ->
            Triple(
                threads.size, // all count
                threads.count { !it.has_spam }, // normal count  
                threads.count { it.has_spam } // spam count
            )
        }
        .distinctUntilChanged()

    init {
        observeThreadsReactively()
        observeThreadCounts()
    }

    private fun observeThreadsReactively() {
        viewModelScope.launch {
            try {
                // Combine cached all threads with current filter for client-side filtering
                combine(
                    allThreadsFlow,
                    _uiState.map { it.currentFilter }.distinctUntilChanged()
                ) { allThreads, currentFilter ->
                    // Client-side filtering - much more efficient
                    val filteredThreads = when (currentFilter) {
                        ThreadFilter.ALL -> allThreads
                        ThreadFilter.NORMAL -> allThreads.filter { !it.has_spam }
                        ThreadFilter.SPAM -> allThreads.filter { it.has_spam }
                    }
                    
                    filteredThreads
                }
                .distinctUntilChanged()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load threads: ${error.message}"
                        )
                    }
                }
                .collect { filteredThreads ->
                    _uiState.update {
                        it.copy(
                            threads = filteredThreads,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load threads: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun observeThreadCounts() {
        viewModelScope.launch {
            threadCountsFlow
                .catch { /* Handle error silently */ }
                .collect { (allCount, normalCount, spamCount) ->
                    _uiState.update {
                        it.copy(
                            allThreadsCount = allCount,
                            normalThreadsCount = normalCount,
                            spamThreadsCount = spamCount
                        )
                    }
                }
        }
    }

    fun setFilter(filter: ThreadFilter) {
        // Only update the filter state - reactive flows will handle the rest
        _uiState.update { it.copy(currentFilter = filter) }
    }

    fun refreshThreads() {
        // Data is already reactive - this could trigger a refresh if needed
        // For pull-to-refresh functionality
    }
    
    // Selection methods
    fun startSelectionMode(threadId: Long) {
        _uiState.update {
            it.copy(
                isSelectionMode = true,
                selectedThreadIds = setOf(threadId)
            )
        }
    }
    
    fun exitSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedThreadIds = emptySet()
            )
        }
    }
    
    fun toggleThreadSelection(threadId: Long) {
        _uiState.update { currentState ->
            val newSelection = if (currentState.selectedThreadIds.contains(threadId)) {
                currentState.selectedThreadIds - threadId
            } else {
                currentState.selectedThreadIds + threadId
            }
            
            // Exit selection mode if no threads selected
            if (newSelection.isEmpty()) {
                currentState.copy(
                    isSelectionMode = false,
                    selectedThreadIds = emptySet()
                )
            } else {
                currentState.copy(selectedThreadIds = newSelection)
            }
        }
    }
    
    fun selectAllThreads() {
        val allThreadIds = _uiState.value.threads.map { it.thread_id }.toSet()
        _uiState.update {
            it.copy(selectedThreadIds = allThreadIds)
        }
    }
    
    fun selectNoneThreads() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedThreadIds = emptySet()
            )
        }
    }
    
    // Delete methods
    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }
    
    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }
    
    fun deleteSelectedThreads() {
        val selectedIds = _uiState.value.selectedThreadIds.toList()
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isDeleting = true,
                    showDeleteConfirmation = false
                ) 
            }
            
            try {
                if (selectedIds.size == 1) {
                    smsRepository.deleteThread(selectedIds.first())
                } else {
                    smsRepository.deleteMultipleThreads(selectedIds)
                }
                
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        isSelectionMode = false,
                        selectedThreadIds = emptySet()
                    )
                }
                
                // Reactive flows will automatically update
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        error = "Failed to delete threads: ${e.message}"
                    )
                }
            }
        }
    }
}
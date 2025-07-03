package com.pandorina.spam_sms_blocker.features.threads

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pandorina.spam_sms_blocker.data.dao.MessageDao
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.ChatScreenDestination
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun ThreadsScreen(
    navigator: DestinationsNavigator,
    viewModel: ThreadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.showDeleteConfirmation) {
        DeleteConfirmationDialog(
            selectedCount = uiState.selectedThreadIds.size,
            onConfirm = { viewModel.deleteSelectedThreads() },
            onDismiss = { viewModel.hideDeleteConfirmation() }
        )
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            shadowElevation = 8.dp
        ) {
            Column {
                if (uiState.isSelectionMode) {
                    SelectionActionBar(
                        selectedCount = uiState.selectedThreadIds.size,
                        totalCount = uiState.threads.size,
                        currentFilter = uiState.currentFilter,
                        onSelectAll = { viewModel.selectAllThreads() },
                        onSelectNone = { viewModel.selectNoneThreads() },
                        onDelete = { viewModel.showDeleteConfirmation() },
                        onExit = { viewModel.exitSelectionMode() }
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Messages",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    TabRow(
                        selectedTabIndex = uiState.currentFilter.ordinal,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Tab(
                            selected = uiState.currentFilter == ThreadFilter.ALL,
                            onClick = { viewModel.setFilter(ThreadFilter.ALL) },
                            text = {
                                Text(
                                    text = "All (${uiState.allThreadsCount})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        )
                        Tab(
                            selected = uiState.currentFilter == ThreadFilter.NORMAL,
                            onClick = { viewModel.setFilter(ThreadFilter.NORMAL) },
                            text = {
                                Text(
                                    text = "Normal (${uiState.normalThreadsCount})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        )
                        Tab(
                            selected = uiState.currentFilter == ThreadFilter.SPAM,
                            onClick = { viewModel.setFilter(ThreadFilter.SPAM) },
                            text = {
                                Text(
                                    text = "Spam (${uiState.spamThreadsCount})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        )
                    }
                }
            }
        }
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "An unknown error occurred",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refreshThreads() }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            uiState.threads.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (uiState.currentFilter) {
                                ThreadFilter.ALL -> "No messages"
                                ThreadFilter.NORMAL -> "No normal messages"
                                ThreadFilter.SPAM -> "No spam messages"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (uiState.currentFilter) {
                                ThreadFilter.ALL -> "You'll see your SMS conversations here"
                                ThreadFilter.NORMAL -> "Normal conversations will appear here"
                                ThreadFilter.SPAM -> "Spam conversations will appear here"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.threads) { thread ->
                        ThreadItem(
                            thread = thread,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = uiState.selectedThreadIds.contains(thread.thread_id),
                            onThreadClick = { threadId ->
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleThreadSelection(threadId)
                                } else {
                                    navigator.navigate(ChatScreenDestination(threadId = threadId))
                                }
                            },
                            onThreadLongClick = { threadId ->
                                if (!uiState.isSelectionMode) {
                                    viewModel.startSelectionMode(threadId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    totalCount: Int,
    currentFilter: ThreadFilter,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onDelete: () -> Unit,
    onExit: () -> Unit
) {
    val filterText = when (currentFilter) {
        ThreadFilter.ALL -> "All"
        ThreadFilter.NORMAL -> "Normal"
        ThreadFilter.SPAM -> "Spam"
    }
    
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount selected from $filterText",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Exit selection"
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    if (selectedCount == totalCount) {
                        onSelectNone()
                    } else {
                        onSelectAll()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.SelectAll,
                    contentDescription = if (selectedCount == totalCount) "Select none" else "Select all"
                )
            }
            
            IconButton(
                onClick = onDelete,
                enabled = selectedCount > 0
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete selected",
                    tint = if (selectedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ThreadItem(
    thread: MessageDao.ThreadData,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onThreadClick: (Long) -> Unit,
    onThreadLongClick: (Long) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .combinedClickable(
                onClick = { onThreadClick(thread.thread_id) },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onThreadLongClick(thread.thread_id)
                }
            ),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            (thread.unread_normal_count + thread.unread_spam_count) > 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            else -> Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onThreadClick(thread.thread_id) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Contact",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = thread.recipient_address,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if ((thread.unread_normal_count + thread.unread_spam_count) > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = formatDate(thread.last_message_date),
                        style = MaterialTheme.typography.bodySmall,
                        color = if ((thread.unread_normal_count + thread.unread_spam_count) > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if ((thread.unread_normal_count + thread.unread_spam_count) > 0) FontWeight.Medium else FontWeight.Normal
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.last_message_body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (thread.unread_normal_count > 0) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (thread.unread_normal_count > 9) "9+" else thread.unread_normal_count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (thread.unread_spam_count > 0) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (thread.unread_spam_count > 9) "9+" else thread.unread_spam_count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onError,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete ${if (selectedCount == 1) "conversation" else "conversations"}?",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = if (selectedCount == 1) {
                    "This conversation and all its messages will be permanently deleted."
                } else {
                    "$selectedCount conversations and all their messages will be permanently deleted."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val calendar = Calendar.getInstance()
    val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    return when {
        diff < 60_000 -> "Now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        calendar.get(Calendar.DAY_OF_YEAR) == messageCalendar.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        calendar.get(Calendar.DAY_OF_YEAR) - 1 == messageCalendar.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
        diff < 604800_000 -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
} 
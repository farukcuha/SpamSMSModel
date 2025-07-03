package com.pandorina.spam_sms_blocker.domain.repository

import com.pandorina.spam_sms_blocker.data.entity.MessageEntity
import com.pandorina.spam_sms_blocker.data.dao.MessageDao
import kotlinx.coroutines.flow.Flow

interface SmsRepository {
    suspend fun syncDeviceSmsWithDatabase()
    suspend fun insertMessages(messages: List<MessageEntity>)
    suspend fun insertMessage(message: MessageEntity)
    suspend fun updateSpamScore(messageId: Long, spamScore: Float)
    suspend fun getMessageById(id: Long): MessageEntity?
    suspend fun getMessagesWithNullSpamScoreLimit(limit: Int): List<MessageEntity>
    suspend fun getMessagesWithNullSpamScoreCount(): Int
    
    // Thread operations
    fun getAllThreads(): Flow<List<MessageDao.ThreadData>>
    fun getNormalThreads(): Flow<List<MessageDao.ThreadData>>
    fun getSpamThreads(): Flow<List<MessageDao.ThreadData>>
    fun getMessagesByThreadId(threadId: Long): Flow<List<MessageEntity>>
    suspend fun getUnreadCountForThread(threadId: Long): Int
    suspend fun markThreadMessagesAsRead(threadId: Long)
    suspend fun deleteThread(threadId: Long)
    suspend fun deleteMultipleThreads(threadIds: List<Long>)
    
    // Thread counts for tabs
    fun getAllThreadsCount(): Flow<Int>
    fun getNormalThreadsCount(): Flow<Int>
    fun getSpamThreadsCount(): Flow<Int>
} 
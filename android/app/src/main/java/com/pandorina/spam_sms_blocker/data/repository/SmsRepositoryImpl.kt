package com.pandorina.spam_sms_blocker.data.repository

import com.pandorina.spam_sms_blocker.data.dao.MessageDao
import com.pandorina.spam_sms_blocker.data.entity.MessageEntity
import com.pandorina.spam_sms_blocker.data.sms.SmsDataSource
import com.pandorina.spam_sms_blocker.domain.repository.SmsRepository
import com.pandorina.spam_sms_blocker.data.spam_detector.SpamDetector
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val smsDataSource: SmsDataSource
) : SmsRepository {

    override suspend fun syncDeviceSmsWithDatabase() {
        val messages = smsDataSource.getAllSmsFromDevice()
        messageDao.insertMessages(messages)
    }

    override suspend fun insertMessages(messages: List<MessageEntity>) {
        messageDao.insertMessages(messages)
    }

    override suspend fun insertMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    override suspend fun updateSpamScore(messageId: Long, spamScore: Float) {
        messageDao.updateSpamScore(messageId, spamScore)
    }

    override suspend fun getMessageById(id: Long): MessageEntity? {
        return messageDao.getMessageById(id)
    }

    override suspend fun getMessagesWithNullSpamScoreLimit(limit: Int): List<MessageEntity> {
        return messageDao.getMessagesWithNullSpamScoreLimit(limit)
    }

    override suspend fun getMessagesWithNullSpamScoreCount(): Int {
        return messageDao.getMessagesWithNullSpamScoreCount()
    }
    
    // Thread operations
    override fun getAllThreads(): Flow<List<MessageDao.ThreadData>> {
        return messageDao.getAllThreads()
    }
    
    override fun getNormalThreads(): Flow<List<MessageDao.ThreadData>> {
        return messageDao.getNormalThreads()
    }
    
    override fun getSpamThreads(): Flow<List<MessageDao.ThreadData>> {
        return messageDao.getSpamThreads()
    }
    
    override fun getMessagesByThreadId(threadId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesByThreadId(threadId)
    }
    
    override suspend fun getUnreadCountForThread(threadId: Long): Int {
        return messageDao.getUnreadCountForThread(threadId)
    }
    
    override suspend fun markThreadMessagesAsRead(threadId: Long) {
        messageDao.markThreadMessagesAsRead(threadId)
    }
    
    override suspend fun deleteThread(threadId: Long) {
        messageDao.deleteThreadMessages(threadId)
    }
    
    override suspend fun deleteMultipleThreads(threadIds: List<Long>) {
        messageDao.deleteMultipleThreads(threadIds)
    }
    
    // Thread counts for tabs
    override fun getAllThreadsCount(): Flow<Int> {
        return messageDao.getAllThreadsCount()
    }
    
    override fun getNormalThreadsCount(): Flow<Int> {
        return messageDao.getNormalThreadsCount()
    }
    
    override fun getSpamThreadsCount(): Flow<Int> {
        return messageDao.getSpamThreadsCount()
    }
} 
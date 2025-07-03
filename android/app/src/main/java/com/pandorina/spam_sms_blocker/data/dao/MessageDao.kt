package com.pandorina.spam_sms_blocker.data.dao

import androidx.room.*
import com.pandorina.spam_sms_blocker.data.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    
    @Query("SELECT * FROM messages ORDER BY date DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE _id = :id")
    suspend fun getMessageById(id: Long): MessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Query("UPDATE messages SET spam_score = :spamScore WHERE _id = :messageId")
    suspend fun updateSpamScore(messageId: Long, spamScore: Float)
    
    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessagesCount(): Int
    
    @Query("SELECT * FROM messages WHERE spam_score IS NULL ORDER BY date DESC LIMIT :limit")
    suspend fun getMessagesWithNullSpamScoreLimit(limit: Int): List<MessageEntity>
    
    @Query("SELECT COUNT(*) FROM messages WHERE spam_score IS NULL")
    suspend fun getMessagesWithNullSpamScoreCount(): Int
    
    // Thread queries
    @Query("""
        WITH latest_messages AS (
            SELECT *,
                ROW_NUMBER() OVER (PARTITION BY thread_id ORDER BY date DESC) as rn
            FROM messages
        ),
        thread_statistics AS (
            SELECT 
                thread_id,
                COUNT(*) as message_count,
                SUM(CASE WHEN read = 0 THEN 1 ELSE 0 END) as unread_count,
                SUM(CASE WHEN read = 0 AND COALESCE(spam_score, 0) <= 0.7 THEN 1 ELSE 0 END) as unread_normal_count,
                SUM(CASE WHEN read = 0 AND COALESCE(spam_score, 0) > 0.7 THEN 1 ELSE 0 END) as unread_spam_count,
                -- Akıllı spam tespiti: Önce okunmamış mesajlara bak
                CASE 
                    -- Eğer okunmamış mesajlar varsa
                    WHEN SUM(CASE WHEN read = 0 THEN 1 ELSE 0 END) > 0 THEN
                        CASE 
                            -- Okunmamış ve spam olmayan mesaj varsa -> Normal thread
                            WHEN SUM(CASE WHEN read = 0 AND COALESCE(spam_score, 0) <= 0.7 THEN 1 ELSE 0 END) > 0 THEN 0
                            -- Sadece okunmamış spam mesajlar varsa -> Spam thread  
                            ELSE 1
                        END
                    -- Okunmamış mesaj yoksa, en son mesaja bak
                    ELSE CASE WHEN MAX(COALESCE(spam_score, 0)) > 0.7 THEN 1 ELSE 0 END
                END as has_spam
            FROM messages 
            GROUP BY thread_id
        )
        SELECT 
            lm.thread_id as thread_id,
            lm.address as recipient_address,
            lm.body as last_message_body,
            lm.date as last_message_date,
            ts.message_count,
            ts.unread_count,
            ts.unread_normal_count,
            ts.unread_spam_count,
            ts.has_spam
        FROM latest_messages lm
        INNER JOIN thread_statistics ts ON lm.thread_id = ts.thread_id
        WHERE lm.rn = 1
        ORDER BY lm.date DESC
    """)
    fun getAllThreads(): Flow<List<ThreadData>>
    
    // Normal threads (not spam)
    @Query("""
        WITH latest_messages AS (
            SELECT *,
                ROW_NUMBER() OVER (PARTITION BY thread_id ORDER BY date DESC) as rn
            FROM messages
        ),
        thread_statistics AS (
            SELECT 
                thread_id,
                COUNT(*) as message_count,
                SUM(CASE WHEN read = 0 THEN 1 ELSE 0 END) as unread_count,
                SUM(CASE WHEN read = 0 AND COALESCE(spam_score, 0) <= 0.7 THEN 1 ELSE 0 END) as unread_normal_count,
                SUM(CASE WHEN read = 0 AND COALESCE(spam_score, 0) > 0.7 THEN 1 ELSE 0 END) as unread_spam_count,
                -- Akıllı spam tespiti: Önce okunmamış mesajlara bak
                CASE 
                    -- Eğer okunmamış mesajlar varsa
                    WHEN SUM(CASE WHEN read = 0 THEN 1 ELSE 0 END) > 0 THEN
                        CASE 
                            -- Okunmamış ve spam olmayan mesaj varsa -> Normal thread
                            WHEN SUM(CASE WHEN read = 0 AND COALESCE(spam_score, 0) <= 0.7 THEN 1 ELSE 0 END) > 0 THEN 0
                            -- Sadece okunmamış spam mesajlar varsa -> Spam thread  
                            ELSE 1
                        END
                    -- Okunmamış mesaj yoksa, en son mesaja bak
                    ELSE CASE WHEN MAX(COALESCE(spam_score, 0)) > 0.7 THEN 1 ELSE 0 END
                END as has_spam
            FROM messages 
            GROUP BY thread_id
            HAVING has_spam = 0
        )
        SELECT 
            lm.thread_id as thread_id,
            lm.address as recipient_address,
            lm.body as last_message_body,
            lm.date as last_message_date,
            ts.message_count,
            ts.unread_count,
            ts.unread_normal_count,
            ts.unread_spam_count,
            ts.has_spam
        FROM latest_messages lm
        INNER JOIN thread_statistics ts ON lm.thread_id = ts.thread_id
        WHERE lm.rn = 1
        ORDER BY lm.date DESC
    """)
    fun getNormalThreads(): Flow<List<ThreadData>>
    
    // Spam threads
    @Query("""
        WITH latest_messages AS (
            SELECT *,
                ROW_NUMBER() OVER (PARTITION BY thread_id ORDER BY date DESC) as rn
            FROM messages
        ),
        thread_statistics AS (
            SELECT 
                thread_id,
                COUNT(*) as message_count,
                SUM(CASE WHEN read = 0 THEN 1 ELSE 0 END) as unread_count,
                SUM(CASE WHEN read = 0 AND COALESCE(spam_score, 0) <= 0.7 THEN 1 ELSE 0 END) as unread_normal_count,
                SUM(CASE WHEN read = 0 AND COALESCE(spam_score, 0) > 0.7 THEN 1 ELSE 0 END) as unread_spam_count,
                -- Akıllı spam tespiti: Önce okunmamış mesajlara bak
                CASE 
                    -- Eğer okunmamış mesajlar varsa
                    WHEN SUM(CASE WHEN read = 0 THEN 1 ELSE 0 END) > 0 THEN
                        CASE 
                            -- Okunmamış ve spam olmayan mesaj varsa -> Normal thread
                            WHEN SUM(CASE WHEN read = 0 AND COALESCE(spam_score, 0) <= 0.7 THEN 1 ELSE 0 END) > 0 THEN 0
                            -- Sadece okunmamış spam mesajlar varsa -> Spam thread  
                            ELSE 1
                        END
                    -- Okunmamış mesaj yoksa, en son mesaja bak
                    ELSE CASE WHEN MAX(COALESCE(spam_score, 0)) > 0.7 THEN 1 ELSE 0 END
                END as has_spam
            FROM messages 
            GROUP BY thread_id
            HAVING has_spam = 1
        )
        SELECT 
            lm.thread_id as thread_id,
            lm.address as recipient_address,
            lm.body as last_message_body,
            lm.date as last_message_date,
            ts.message_count,
            ts.unread_count,
            ts.unread_normal_count,
            ts.unread_spam_count,
            ts.has_spam
        FROM latest_messages lm
        INNER JOIN thread_statistics ts ON lm.thread_id = ts.thread_id
        WHERE lm.rn = 1
        ORDER BY lm.date DESC
    """)
    fun getSpamThreads(): Flow<List<ThreadData>>
    
    // Thread counts for tabs
    @Query("""
        SELECT COUNT(DISTINCT thread_id) 
        FROM messages
    """)
    fun getAllThreadsCount(): Flow<Int>
    
    @Query("""
        SELECT COUNT(DISTINCT thread_id) 
        FROM messages 
        WHERE thread_id IN (
            SELECT thread_id 
            FROM messages 
            GROUP BY thread_id 
            HAVING 
                CASE 
                    -- Eğer okunmamış mesajlar varsa
                    WHEN SUM(CASE WHEN read = 0 THEN 1 ELSE 0 END) > 0 THEN
                        CASE 
                            -- Okunmamış ve spam olmayan mesaj varsa -> Normal thread
                            WHEN SUM(CASE WHEN read = 0 AND COALESCE(spam_score, 0) <= 0.7 THEN 1 ELSE 0 END) > 0 THEN 0
                            -- Sadece okunmamış spam mesajlar varsa -> Spam thread  
                            ELSE 1
                        END
                    -- Okunmamış mesaj yoksa, en son mesaja bak
                    ELSE CASE WHEN MAX(COALESCE(spam_score, 0)) > 0.7 THEN 1 ELSE 0 END
                END = 0
        )
    """)
    fun getNormalThreadsCount(): Flow<Int>
    
    @Query("""
        SELECT COUNT(DISTINCT thread_id) 
        FROM messages 
        WHERE thread_id IN (
            SELECT thread_id 
            FROM messages 
            GROUP BY thread_id 
            HAVING 
                CASE 
                    -- Eğer okunmamış mesajlar varsa
                    WHEN SUM(CASE WHEN read = 0 THEN 1 ELSE 0 END) > 0 THEN
                        CASE 
                            -- Okunmamış ve spam olmayan mesaj varsa -> Normal thread
                            WHEN SUM(CASE WHEN read = 0 AND COALESCE(spam_score, 0) <= 0.7 THEN 1 ELSE 0 END) > 0 THEN 0
                            -- Sadece okunmamış spam mesajlar varsa -> Spam thread  
                            ELSE 1
                        END
                    -- Okunmamış mesaj yoksa, en son mesaja bak
                    ELSE CASE WHEN MAX(COALESCE(spam_score, 0)) > 0.7 THEN 1 ELSE 0 END
                END = 1
        )
    """)
    fun getSpamThreadsCount(): Flow<Int>
    
    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY date ASC")
    fun getMessagesByThreadId(threadId: Long): Flow<List<MessageEntity>>
    
    @Query("SELECT COUNT(*) FROM messages WHERE thread_id = :threadId AND read = 0")
    suspend fun getUnreadCountForThread(threadId: Long): Int
    
    @Query("UPDATE messages SET read = 1 WHERE thread_id = :threadId AND read = 0")
    suspend fun markThreadMessagesAsRead(threadId: Long)
    
    @Query("DELETE FROM messages WHERE thread_id = :threadId")
    suspend fun deleteThreadMessages(threadId: Long)
    
    @Query("DELETE FROM messages WHERE thread_id IN (:threadIds)")
    suspend fun deleteMultipleThreads(threadIds: List<Long>)
    
    data class ThreadData(
        val thread_id: Long,
        val recipient_address: String,
        val last_message_body: String,
        val last_message_date: Long,
        val message_count: Int,
        val unread_count: Int,
        val unread_normal_count: Int,
        val unread_spam_count: Int,
        val has_spam: Boolean
    )
} 
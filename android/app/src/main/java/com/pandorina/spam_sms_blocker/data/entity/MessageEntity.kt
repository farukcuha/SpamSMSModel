package com.pandorina.spam_sms_blocker.data.entity

import android.content.Context
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0,
    
    @ColumnInfo(name = "thread_id")
    val threadId: Long,
    
    @ColumnInfo(name = "address")
    val address: String,
    
    @ColumnInfo(name = "body")
    val body: String,
    
    @ColumnInfo(name = "date")
    val date: Long,
    
    @ColumnInfo(name = "type")
    val type: Int,
    
    @ColumnInfo(name = "read")
    val read: Boolean,
    
    @ColumnInfo(name = "seen")
    val seen: Boolean,
    
    @ColumnInfo(name = "spam_score")
    val spamScore: Float? = null
) {

    companion object {

        fun from(context: Context, smsMessage: SmsMessage): MessageEntity {
            val recipients = setOf(smsMessage.originatingAddress)
            val threadId = Telephony.Threads.getOrCreateThreadId(context, recipients)
            return MessageEntity(
                threadId = threadId,
                address = smsMessage.originatingAddress ?: "",
                body = smsMessage.messageBody ?: "",
                date = smsMessage.timestampMillis,
                type = Telephony.Sms.MESSAGE_TYPE_INBOX,
                read = false,
                seen = false,
                spamScore = null
            )
        }
    }
}
package com.pandorina.spam_sms_blocker.data.sms

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import com.pandorina.spam_sms_blocker.data.entity.MessageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceSmsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : SmsDataSource {

    override suspend fun getAllSmsFromDevice(): List<MessageEntity> {
        val messages = mutableListOf<MessageEntity>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            null,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )
        
        cursor?.use { 
            while (it.moveToNext()) {
                val message = mapCursorToMessageEntity(it)
                messages.add(message)
            }
        }
        
        return messages
    }

    private fun mapCursorToMessageEntity(cursor: Cursor): MessageEntity {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
        val threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
        val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
        val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
        val date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
        val type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE))
        val read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
        val seen = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.SEEN)) == 1

        return MessageEntity(
            id = id,
            threadId = threadId,
            address = address,
            body = body,
            date = date,
            type = type,
            read = read,
            seen = seen,
            spamScore = null
        )
    }
} 
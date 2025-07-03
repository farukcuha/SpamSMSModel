package com.pandorina.spam_sms_blocker.data.sms

import com.pandorina.spam_sms_blocker.data.entity.MessageEntity

interface SmsDataSource {
    suspend fun getAllSmsFromDevice(): List<MessageEntity>
}
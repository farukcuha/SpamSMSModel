package com.pandorina.spam_sms_blocker.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.pandorina.spam_sms_blocker.data.entity.MessageEntity
import com.pandorina.spam_sms_blocker.domain.repository.SmsRepository
import com.pandorina.spam_sms_blocker.services.SpamDetectionServiceHelper
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION || 
            intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages != null && messages.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val appContext = context.applicationContext
                        val entryPoint = EntryPointAccessors.fromApplication(
                            appContext,
                            SmsReceiverEntryPoint::class.java
                        )
                        val smsRepository = entryPoint.smsRepository()
                        val spamDetectionHelper = entryPoint.spamDetectionServiceHelper()
                        messages.forEach { smsMessage ->
                            val messageEntity = MessageEntity.from(context, smsMessage)
                            smsRepository.insertMessage(messageEntity)
                            spamDetectionHelper.processPendingMessages(appContext)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmsReceiverEntryPoint {
    fun smsRepository(): SmsRepository
    fun spamDetectionServiceHelper(): SpamDetectionServiceHelper
} 
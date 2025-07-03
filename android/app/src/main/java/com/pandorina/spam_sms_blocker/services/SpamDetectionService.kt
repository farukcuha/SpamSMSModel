package com.pandorina.spam_sms_blocker.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.pandorina.spam_sms_blocker.data.spam_detector.SpamDetectionResult
import com.pandorina.spam_sms_blocker.data.spam_detector.SpamDetector
import com.pandorina.spam_sms_blocker.domain.repository.SmsRepository
import com.pandorina.spam_sms_blocker.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SpamDetectionService : Service() {

    @Inject
    lateinit var spamDetector: SpamDetector

    @Inject
    lateinit var smsRepository: SmsRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isProcessing = false

    companion object {
        const val ACTION_PROCESS_PENDING_MESSAGES = "com.pandorina.spam_sms_blocker.PROCESS_PENDING_MESSAGES"
        const val EXTRA_SHOW_NOTIFICATION = "show_notification"
        private const val TAG = "SpamDetectionService"
        private const val BATCH_SIZE = 10
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PROCESS_PENDING_MESSAGES -> {
                val showNotification = intent.getBooleanExtra(EXTRA_SHOW_NOTIFICATION, true)
                processPendingMessages(showNotification)
            }
        }
        return START_NOT_STICKY
    }

    private fun processPendingMessages(showNotification: Boolean) {
        if (isProcessing) {
            Log.d(TAG, "Already processing messages, skipping...")
            return
        }

        serviceScope.launch {
            isProcessing = true
            try {
                if (!spamDetector.isReady()) {
                    Log.d(TAG, "Initializing spam detector...")
                    spamDetector.initialize()
                }

                if (!spamDetector.isReady()) {
                    Log.e(TAG, "Spam detector could not be initialized")
                    return@launch
                }
                var processedCount = 0
                var hasMoreMessages = true
                while (hasMoreMessages) {
                    val pendingMessages = smsRepository.getMessagesWithNullSpamScoreLimit(BATCH_SIZE)
                    hasMoreMessages = pendingMessages.size == BATCH_SIZE
                    if (pendingMessages.isEmpty()) {
                        Log.d(TAG, "No more pending messages to process")
                        break
                    }
                    for (message in pendingMessages) {
                        try {
                            val result = spamDetector.detectSpam(message.body)
                            when (result) {
                                is SpamDetectionResult.Success -> {
                                    smsRepository.updateSpamScore(message.id, result.probability)
                                    val updatedMessage = smsRepository.getMessageById(message.id)
                                    if (showNotification) updatedMessage?.let {
                                        notificationHelper.showSmsNotification(it)
                                    }
                                    processedCount++
                                }
                                is SpamDetectionResult.Error -> {
                                    Log.e(TAG, "Error detecting spam for message ${message.id}: ${result.message}")
                                }
                                is SpamDetectionResult.NotInitialized -> {
                                    hasMoreMessages = false
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception processing message ${message.id}", e)
                        }
                        delay(50)
                    }
                    if (hasMoreMessages) delay(1000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in processPendingMessages", e)
            } finally {
                isProcessing = false
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
} 
package com.pandorina.spam_sms_blocker.services

import android.content.Context
import android.content.Intent
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpamDetectionServiceHelper @Inject constructor() {

    companion object {
        private const val TAG = "SpamDetectionHelper"
    }

    fun processPendingMessages(context: Context, showNotification: Boolean = true) {
        try {
            val intent = Intent(context, SpamDetectionService::class.java).apply {
                action = SpamDetectionService.ACTION_PROCESS_PENDING_MESSAGES
                putExtra(SpamDetectionService.EXTRA_SHOW_NOTIFICATION, showNotification)
            }
            context.startService(intent)
            Log.d(TAG, "Started spam detection service for pending messages")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start spam detection service for pending messages", e)
        }
    }
} 
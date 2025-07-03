package com.pandorina.spam_sms_blocker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pandorina.spam_sms_blocker.data.entity.MessageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val SMS_CHANNEL_ID = "sms_notifications"
        private const val SMS_CHANNEL_NAME = "SMS Notifications"
        private const val SMS_CHANNEL_DESCRIPTION = "Notifications for incoming SMS messages"
        private const val SPAM_THRESHOLD = 0.7f
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SMS_CHANNEL_ID,
                SMS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = SMS_CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSmsNotification(message: MessageEntity) {
        val spamScore = message.spamScore ?: 0f
        if (spamScore >= SPAM_THRESHOLD) {
            return
        }

        val notification = NotificationCompat.Builder(context, SMS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(message.address)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(message.id.toInt(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
} 
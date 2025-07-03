package com.pandorina.spam_sms_blocker.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.pandorina.spam_sms_blocker.data.spam_detector.SpamDetector
import com.pandorina.spam_sms_blocker.domain.repository.SmsRepository
import com.pandorina.spam_sms_blocker.permissions.SMSPermissionManager
import com.pandorina.spam_sms_blocker.services.SpamDetectionServiceHelper
import com.pandorina.spam_sms_blocker.theme.SpamSMSBlockerTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: SMSPermissionManager

    @Inject
    lateinit var spamDetector: SpamDetector

    @Inject
    lateinit var smsRepository: SmsRepository

    @Inject
    lateinit var spamDetectionServiceHelper: SpamDetectionServiceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SpamSMSBlockerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DestinationsNavHost(navGraph = NavGraphs.root)
                }
            }
        }
        
        handlePermission()
    }

    private fun handlePermission() {
        permissionManager.initialize(this)
        permissionManager.ensureDefaultRole()
        permissionManager.setOnRoleGrantedCallback {
            startSync()
        }
    }

    private fun startSync() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                smsRepository.syncDeviceSmsWithDatabase()
                spamDetector.initialize()
                val pendingCount = smsRepository.getMessagesWithNullSpamScoreCount()
                if (pendingCount > 0) {
                    spamDetectionServiceHelper.processPendingMessages(
                        this@MainActivity,
                        showNotification = false
                    )
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "SMS sync failed", e)
            }
        }
    }
}

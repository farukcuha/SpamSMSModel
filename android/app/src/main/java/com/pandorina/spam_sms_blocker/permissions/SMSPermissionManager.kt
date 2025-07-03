package com.pandorina.spam_sms_blocker.permissions

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SMSPermissionManager @Inject constructor() {

    private lateinit var activity: ComponentActivity
    private lateinit var requestRoleLauncher: ActivityResultLauncher<Intent>

    private var onRoleGrantedCallback: (() -> Unit)? = null

    fun setOnRoleGrantedCallback(callback: () -> Unit) {
        this.onRoleGrantedCallback = callback
    }

    fun initialize(activity: ComponentActivity) {
        this.activity = activity
        setupLaunchers()
    }

    private fun setupLaunchers() {
        requestRoleLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            onRoleGrantedCallback?.invoke()
        }
    }

    private fun isDefaultSmsApp(): Boolean =
        Telephony.Sms.getDefaultSmsPackage(activity) == activity.packageName

    fun ensureDefaultRole() {
        if (isDefaultSmsApp()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                requestRoleLauncher.launch(intent)
            }
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.packageName)
                activity.startActivity(this)
            }
        }
    }
}
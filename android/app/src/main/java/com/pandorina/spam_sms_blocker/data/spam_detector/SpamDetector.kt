package com.pandorina.spam_sms_blocker.data.spam_detector

sealed class SpamDetectionResult {
    data class Success(val probability: Float) : SpamDetectionResult()
    data object NotInitialized : SpamDetectionResult()
    data class Error(val message: String) : SpamDetectionResult()
}

interface SpamDetector {
    suspend fun initialize()

    fun isReady(): Boolean

    fun detectSpam(message: String): SpamDetectionResult
}
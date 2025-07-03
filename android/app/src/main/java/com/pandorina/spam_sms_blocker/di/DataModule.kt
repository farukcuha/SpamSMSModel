package com.pandorina.spam_sms_blocker.di

import com.pandorina.spam_sms_blocker.data.sms.DeviceSmsDataSource
import com.pandorina.spam_sms_blocker.data.sms.SmsDataSource
import com.pandorina.spam_sms_blocker.data.spam_detector.SpamDetector
import com.pandorina.spam_sms_blocker.data.spam_detector.SpamDetectorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSmsDataSource(
        deviceSmsDataSource: DeviceSmsDataSource
    ): SmsDataSource

    @Binds
    @Singleton
    abstract fun bindSpamDetector(
        spamDetectorImpl: SpamDetectorImpl
    ): SpamDetector
}
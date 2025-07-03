package com.pandorina.spam_sms_blocker.di

import com.pandorina.spam_sms_blocker.domain.repository.SmsRepository
import com.pandorina.spam_sms_blocker.data.repository.SmsRepositoryImpl
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
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSmsRepository(
        smsRepositoryImpl: SmsRepositoryImpl
    ): SmsRepository
} 
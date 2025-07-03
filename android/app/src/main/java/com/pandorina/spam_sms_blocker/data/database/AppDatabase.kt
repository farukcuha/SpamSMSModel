package com.pandorina.spam_sms_blocker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.pandorina.spam_sms_blocker.data.dao.MessageDao
import com.pandorina.spam_sms_blocker.data.entity.MessageEntity

@Database(
    entities = [MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun messageDao(): MessageDao
    
    companion object {
        const val DATABASE_NAME = "spam_sms_blocker_database"
    }
} 
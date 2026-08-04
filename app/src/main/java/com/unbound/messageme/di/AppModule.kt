package com.unbound.messageme.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.unbound.messageme.BuildConfig
import com.unbound.messageme.data.local.AppDatabase
import com.unbound.messageme.data.local.ChatMessageDao
import com.unbound.messageme.data.local.ScheduledReminderDao
import com.unbound.messageme.data.local.SyncQueueDao
import com.unbound.messageme.data.local.TaskDao
import com.unbound.messageme.data.sync.CloudSync
import com.unbound.messageme.data.sync.FirebaseCloudSync
import com.unbound.messageme.data.sync.NoOpCloudSync
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "messageme.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideChatDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()
    @Provides fun provideReminderDao(db: AppDatabase): ScheduledReminderDao = db.scheduledReminderDao()
    @Provides fun provideSyncQueueDao(db: AppDatabase): SyncQueueDao = db.syncQueueDao()

    @Provides
    @Singleton
    fun provideCloudSync(@ApplicationContext context: Context): CloudSync {
        if (!BuildConfig.FIREBASE_ENABLED) return NoOpCloudSync()
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            if (FirebaseApp.getApps(context).isEmpty()) NoOpCloudSync() else FirebaseCloudSync()
        } catch (_: Exception) {
            NoOpCloudSync()
        }
    }
}

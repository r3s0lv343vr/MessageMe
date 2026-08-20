package com.unbound.messageme.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id AND deleted = 0 LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY dueAtEpochMillis ASC")
    fun observeActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY dueAtEpochMillis ASC")
    suspend fun getActive(): List<TaskEntity>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deleted = 0
          AND (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY dueAtEpochMillis ASC
        """
    )
    fun search(query: String): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE deleted = 0 AND status = 'COMPLETED'")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE deleted = 0 AND status != 'COMPLETED' AND status != 'DISMISSED'")
    fun observeOpenCount(): Flow<Int>
}

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatMessageEntity)

    @Update
    suspend fun update(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages ORDER BY sentAtEpochMillis ASC, id ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChatMessageEntity?

    @Query("UPDATE chat_messages SET isUnread = 0 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE chat_messages SET isUnread = 0 WHERE taskId = :taskId AND isUnread = 1")
    suspend fun markTaskMessagesRead(taskId: String)

    @Query("DELETE FROM chat_messages WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: String)
}

@Dao
interface ScheduledReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reminders: List<ScheduledReminderEntity>)

    @Update
    suspend fun update(reminder: ScheduledReminderEntity)

    @Query("SELECT * FROM scheduled_reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ScheduledReminderEntity?

    @Query(
        """
        SELECT * FROM scheduled_reminders
        WHERE cancelled = 0 AND delivered = 0
        ORDER BY triggerAtEpochMillis ASC
        """
    )
    suspend fun getPending(): List<ScheduledReminderEntity>

    @Query(
        """
        SELECT * FROM scheduled_reminders
        WHERE cancelled = 0 AND delivered = 0 AND triggerAtEpochMillis <= :nowEpochMillis
        ORDER BY triggerAtEpochMillis ASC
        """
    )
    suspend fun getOverdue(nowEpochMillis: Long): List<ScheduledReminderEntity>

    @Query(
        """
        SELECT * FROM scheduled_reminders
        WHERE taskId = :taskId AND cancelled = 0
        ORDER BY triggerAtEpochMillis ASC
        """
    )
    suspend fun getForTask(taskId: String): List<ScheduledReminderEntity>

    @Query(
        """
        UPDATE scheduled_reminders SET cancelled = 1
        WHERE taskId = :taskId AND delivered = 0 AND cancelled = 0
        """
    )
    suspend fun cancelPendingForTask(taskId: String)

    @Query(
        """
        UPDATE scheduled_reminders SET cancelled = 1
        WHERE taskId = :taskId AND delivered = 0 AND cancelled = 0
          AND type IN ('UNACKED_1','UNACKED_2','UNACKED_3')
        """
    )
    suspend fun cancelUnackedForTask(taskId: String)
}

@Dao
interface SyncQueueDao {
    @Insert
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY createdAtEpochMillis ASC")
    suspend fun all(): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sync_queue")
    suspend fun clear()
}

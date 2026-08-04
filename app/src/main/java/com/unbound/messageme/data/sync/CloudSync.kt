package com.unbound.messageme.data.sync

import com.unbound.messageme.data.local.TaskEntity

interface CloudSync {
    val isConfigured: Boolean
    suspend fun signInAnonymously(): Result<String>
    suspend fun signInWithEmail(email: String, password: String): Result<String>
    suspend fun signOut()
    suspend fun currentUserId(): String?
    suspend fun pushTasks(tasks: List<TaskEntity>): Result<Unit>
    suspend fun pullTasks(): Result<List<TaskEntity>>
}

/**
 * Offline-first no-op sync used when Firebase is not configured
 * (no google-services.json / FirebaseApp not initialized).
 */
class NoOpCloudSync : CloudSync {
    override val isConfigured: Boolean = false
    override suspend fun signInAnonymously() = Result.failure<String>(IllegalStateException("Firebase not configured"))
    override suspend fun signInWithEmail(email: String, password: String) =
        Result.failure<String>(IllegalStateException("Firebase not configured"))
    override suspend fun signOut() = Unit
    override suspend fun currentUserId(): String? = null
    override suspend fun pushTasks(tasks: List<TaskEntity>) =
        Result.failure<Unit>(IllegalStateException("Firebase not configured"))
    override suspend fun pullTasks() =
        Result.failure<List<TaskEntity>>(IllegalStateException("Firebase not configured"))
}

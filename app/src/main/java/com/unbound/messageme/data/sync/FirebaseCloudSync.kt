package com.unbound.messageme.data.sync

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.unbound.messageme.data.local.Priority
import com.unbound.messageme.data.local.Recurrence
import com.unbound.messageme.data.local.TaskEntity
import com.unbound.messageme.data.local.TaskStatus
import kotlinx.coroutines.tasks.await

class FirebaseCloudSync : CloudSync {
    override val isConfigured: Boolean
        get() = runCatching { FirebaseApp.getInstance(); true }.getOrDefault(false)

    private val auth: FirebaseAuth? get() = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val db: FirebaseFirestore? get() = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    override suspend fun signInAnonymously(): Result<String> = runCatching {
        val result = auth?.signInAnonymously()?.await()
            ?: error("Firebase Auth unavailable")
        result.user?.uid ?: error("No user")
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<String> = runCatching {
        val result = auth?.signInWithEmailAndPassword(email, password)?.await()
            ?: error("Firebase Auth unavailable")
        result.user?.uid ?: error("No user")
    }

    override suspend fun signOut() {
        auth?.signOut()
    }

    override suspend fun currentUserId(): String? = auth?.currentUser?.uid

    override suspend fun pushTasks(tasks: List<TaskEntity>): Result<Unit> = runCatching {
        val uid = currentUserId() ?: error("Not signed in")
        val firestore = db ?: error("Firestore unavailable")
        tasks.forEach { task ->
            firestore.collection("users").document(uid)
                .collection("tasks").document(task.id)
                .set(task.toMap(), SetOptions.merge())
                .await()
        }
    }

    override suspend fun pullTasks(): Result<List<TaskEntity>> = runCatching {
        val uid = currentUserId() ?: error("Not signed in")
        val firestore = db ?: error("Firestore unavailable")
        val snap = firestore.collection("users").document(uid)
            .collection("tasks").get().await()
        snap.documents.mapNotNull { it.data?.toTask(it.id) }
    }

    private fun TaskEntity.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "body" to body,
        "dueAtEpochMillis" to dueAtEpochMillis,
        "createdAtEpochMillis" to createdAtEpochMillis,
        "updatedAtEpochMillis" to updatedAtEpochMillis,
        "status" to status.name,
        "priority" to priority.name,
        "category" to category,
        "recurrence" to recurrence.name,
        "customRecurrenceDays" to customRecurrenceDays,
        "timeWasExplicitlyChosen" to timeWasExplicitlyChosen,
        "acknowledgedAtEpochMillis" to acknowledgedAtEpochMillis,
        "completedAtEpochMillis" to completedAtEpochMillis,
        "snoozeUntilEpochMillis" to snoozeUntilEpochMillis,
        "deleted" to deleted
    )

    private fun Map<String, Any?>.toTask(docId: String): TaskEntity {
        fun long(key: String) = (this[key] as? Number)?.toLong()
        return TaskEntity(
            id = (this["id"] as? String) ?: docId,
            title = this["title"] as? String ?: "",
            body = this["body"] as? String ?: "",
            dueAtEpochMillis = long("dueAtEpochMillis") ?: 0L,
            createdAtEpochMillis = long("createdAtEpochMillis") ?: 0L,
            updatedAtEpochMillis = long("updatedAtEpochMillis") ?: 0L,
            status = runCatching { TaskStatus.valueOf(this["status"] as String) }.getOrDefault(TaskStatus.PENDING),
            priority = runCatching { Priority.valueOf(this["priority"] as String) }.getOrDefault(Priority.NORMAL),
            category = this["category"] as? String ?: "General",
            recurrence = runCatching { Recurrence.valueOf(this["recurrence"] as String) }.getOrDefault(Recurrence.NONE),
            customRecurrenceDays = (this["customRecurrenceDays"] as? Number)?.toInt(),
            timeWasExplicitlyChosen = this["timeWasExplicitlyChosen"] as? Boolean ?: false,
            acknowledgedAtEpochMillis = long("acknowledgedAtEpochMillis"),
            completedAtEpochMillis = long("completedAtEpochMillis"),
            snoozeUntilEpochMillis = long("snoozeUntilEpochMillis"),
            deleted = this["deleted"] as? Boolean ?: false,
            firebaseDocId = docId
        )
    }
}

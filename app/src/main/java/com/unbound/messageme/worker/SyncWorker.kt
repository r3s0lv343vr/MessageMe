package com.unbound.messageme.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unbound.messageme.data.repository.MessageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MessageRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return repository.syncNow().fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                // Do not spin forever when Firebase is intentionally unset.
                if (error.message?.contains("not configured", ignoreCase = true) == true) {
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        )
    }
}

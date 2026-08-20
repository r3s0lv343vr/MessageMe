package com.unbound.messageme.domain

import com.unbound.messageme.data.local.TaskEntity

/** Lock-screen / shade copy: the note itself, as a text from you. */
object NotificationCopy {
    const val SENDER = "You"

    fun personalNote(task: TaskEntity): String {
        val title = task.title.trim()
        val body = task.body.trim()
        return when {
            body.isNotEmpty() && title.isNotEmpty() && !body.contains(title, ignoreCase = true) ->
                "$title\n$body"
            body.isNotEmpty() -> body
            else -> title
        }
    }

    fun notificationId(taskId: String, reminderId: String): Int =
        (taskId + reminderId).hashCode()
}

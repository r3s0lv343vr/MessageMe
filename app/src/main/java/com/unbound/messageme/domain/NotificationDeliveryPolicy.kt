package com.unbound.messageme.domain

import com.unbound.messageme.data.local.TaskStatus

/**
 * Decides whether a delivered in-app reminder should also post a system notification.
 * Internal toggle off keeps the chat/Room reminder but pauses envelope delivery.
 */
object NotificationDeliveryPolicy {
    fun shouldShowSystemNotification(
        internalNotificationsEnabled: Boolean,
        taskDeleted: Boolean,
        status: TaskStatus
    ): Boolean {
        if (!internalNotificationsEnabled) return false
        if (taskDeleted) return false
        return status != TaskStatus.DISMISSED && status != TaskStatus.COMPLETED
    }
}

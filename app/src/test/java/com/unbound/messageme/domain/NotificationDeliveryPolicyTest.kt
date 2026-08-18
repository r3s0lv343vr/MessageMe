package com.unbound.messageme.domain

import com.google.common.truth.Truth.assertThat
import com.unbound.messageme.data.local.TaskStatus
import org.junit.Test

class NotificationDeliveryPolicyTest {
    @Test
    fun pausesWhenInternalToggleOff() {
        assertThat(
            NotificationDeliveryPolicy.shouldShowSystemNotification(
                internalNotificationsEnabled = false,
                taskDeleted = false,
                status = TaskStatus.PENDING
            )
        ).isFalse()
    }

    @Test
    fun deliversWhenToggleOnAndTaskOpen() {
        assertThat(
            NotificationDeliveryPolicy.shouldShowSystemNotification(
                internalNotificationsEnabled = true,
                taskDeleted = false,
                status = TaskStatus.ACKNOWLEDGED
            )
        ).isTrue()
    }

    @Test
    fun skipsDeletedCompletedAndDismissed() {
        assertThat(
            NotificationDeliveryPolicy.shouldShowSystemNotification(true, true, TaskStatus.PENDING)
        ).isFalse()
        assertThat(
            NotificationDeliveryPolicy.shouldShowSystemNotification(true, false, TaskStatus.COMPLETED)
        ).isFalse()
        assertThat(
            NotificationDeliveryPolicy.shouldShowSystemNotification(true, false, TaskStatus.DISMISSED)
        ).isFalse()
    }
}

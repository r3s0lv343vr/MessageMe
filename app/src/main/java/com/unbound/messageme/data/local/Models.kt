package com.unbound.messageme.data.local

enum class TaskStatus {
    PENDING,
    ACKNOWLEDGED,
    COMPLETED,
    NEEDS_RESCHEDULE,
    SHELVED_UNACKNOWLEDGED,
    DISMISSED
}

enum class MessageKind {
    USER_COMPOSE,
    REMINDER,
    DAYTIME_REMINDER,
    FOLLOW_UP_UNACKED,
    COMPLETION_CHECK,
    RESCHEDULE_REQUEST,
    SYSTEM
}

enum class ReminderType {
    T_MINUS_3H,
    T_MINUS_1H,
    T_MINUS_30M,
    T_MINUS_5M,
    AT_DUE,
    DAYTIME_8AM,
    DAYTIME_10AM,
    DAYTIME_3PM,
    UNACKED_1,
    UNACKED_2,
    UNACKED_3,
    COMPLETION_CHECK,
    COMPLETION_CHECK_RETRY,
    RESCHEDULE_REQUEST
}

enum class Priority {
    LOW,
    NORMAL,
    HIGH
}

enum class Recurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM
}

enum class CalendarDayStatus {
    FREE,
    HAS_PENDING,
    COMPLETED,
    OVERDUE,
    MIXED,
    UNOPENED,
    ACKNOWLEDGED_UNFINISHED
}

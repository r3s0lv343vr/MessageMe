package com.unbound.messageme.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromTaskStatus(v: TaskStatus): String = v.name
    @TypeConverter fun toTaskStatus(v: String): TaskStatus = TaskStatus.valueOf(v)
    @TypeConverter fun fromMessageKind(v: MessageKind): String = v.name
    @TypeConverter fun toMessageKind(v: String): MessageKind = MessageKind.valueOf(v)
    @TypeConverter fun fromReminderType(v: ReminderType): String = v.name
    @TypeConverter fun toReminderType(v: String): ReminderType = ReminderType.valueOf(v)
    @TypeConverter fun fromPriority(v: Priority): String = v.name
    @TypeConverter fun toPriority(v: String): Priority = Priority.valueOf(v)
    @TypeConverter fun fromRecurrence(v: Recurrence): String = v.name
    @TypeConverter fun toRecurrence(v: String): Recurrence = Recurrence.valueOf(v)
}

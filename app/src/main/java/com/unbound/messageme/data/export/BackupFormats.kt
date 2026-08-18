package com.unbound.messageme.data.export

import com.google.gson.GsonBuilder
import com.unbound.messageme.data.local.TaskEntity

object BackupFormats {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun toJson(tasks: List<TaskEntity>, exportedAtEpochMillis: Long): String {
        val payload = BackupPayload(exportedAtEpochMillis = exportedAtEpochMillis, tasks = tasks)
        return gson.toJson(payload)
    }

    fun fromJson(json: String): BackupPayload =
        gson.fromJson(json, BackupPayload::class.java)

    fun toCsv(tasks: List<TaskEntity>): String {
        val header = "id,title,body,dueAt,status,priority,category,recurrence"
        val rows = tasks.joinToString("\n") { t ->
            listOf(
                t.id,
                escape(t.title),
                escape(t.body),
                t.dueAtEpochMillis.toString(),
                t.status.name,
                t.priority.name,
                escape(t.category),
                t.recurrence.name
            ).joinToString(",")
        }
        return if (rows.isEmpty()) header else "$header\n$rows"
    }

    fun escape(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
}

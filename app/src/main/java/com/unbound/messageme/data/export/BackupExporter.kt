package com.unbound.messageme.data.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.unbound.messageme.data.local.TaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class BackupPayload(
    val version: Int = 1,
    val exportedAtEpochMillis: Long,
    val tasks: List<TaskEntity>
)

@Singleton
class BackupExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun exportJson(tasks: List<TaskEntity>): File {
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, "messageme-backup-${System.currentTimeMillis()}.json")
        file.writeText(BackupFormats.toJson(tasks, System.currentTimeMillis()))
        return file
    }

    fun importJson(json: String): BackupPayload = BackupFormats.fromJson(json)

    fun exportCsv(tasks: List<TaskEntity>): File {
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, "messageme-tasks-${System.currentTimeMillis()}.csv")
        file.writeText(BackupFormats.toCsv(tasks))
        return file
    }

    fun exportPdf(tasks: List<TaskEntity>): File {
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, "messageme-tasks-${System.currentTimeMillis()}.pdf")
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        var y = 40f
        canvas.drawText("MessageMe task export", 40f, y, titlePaint)
        y += 28f
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
        tasks.forEach { task ->
            if (y > 800f) {
                doc.finishPage(page)
                page = doc.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }
            val due = fmt.format(Instant.ofEpochMilli(task.dueAtEpochMillis))
            canvas.drawText("${task.title} · $due · ${task.status}", 40f, y, bodyPaint)
            y += 18f
        }
        doc.finishPage(page)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}

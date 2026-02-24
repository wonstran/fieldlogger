package com.fieldlogger.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.fieldlogger.domain.model.Event
import javax.inject.Inject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter @Inject constructor(
    private val context: Context
) {
    fun exportToCsv(events: List<Event>, customFileName: String? = null): Result<String> {
        return try {
            val fileName = customFileName?.takeIf { it.isNotBlank() }
                ?: generateFileName()
            val csvContent = buildCsvContent(events)

            val outputStream: OutputStream
            val filePath: String

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return Result.failure(Exception("Failed to create file"))

                outputStream = context.contentResolver.openOutputStream(uri)
                    ?: return Result.failure(Exception("Failed to open output stream"))
                filePath = "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
                filePath = file.absolutePath
            }

            outputStream.use { stream ->
                stream.write(csvContent.toByteArray())
            }

            Result.success(filePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportToCsvForShare(events: List<Event>): Result<Uri> {
        return try {
            val fileName = generateFileName()
            val csvContent = buildCsvContent(events)

            val file = File(context.cacheDir, fileName)
            file.writeText(csvContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareFile(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun generateFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "fieldlogger_export_$timestamp.csv"
    }

    private fun buildCsvContent(events: List<Event>): String {
        val sb = StringBuilder()
        sb.appendLine("Index,Event Name,Timestamp,Latitude,Longitude,Note")

        events.sortedBy { it.eventIndex }.forEach { event ->
            sb.appendLine(
                "${event.eventIndex}," +
                "\"${escapeCsv(event.eventName)}\"," +
                "${event.timestamp}," +
                "${event.latitude}," +
                "${event.longitude}," +
                "\"${escapeCsv(event.note)}\""
            )
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}

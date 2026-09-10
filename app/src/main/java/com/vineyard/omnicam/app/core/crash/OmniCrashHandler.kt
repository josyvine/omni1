package com.vineyard.omnicam.app.core.crash

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OmniCrashHandler: Intercepts all uncaught exceptions and writes comprehensive
 * crash logs directly to the public SD card / external storage directory:
 *   /sdcard/omni log/   or   Environment.getExternalStorageDirectory()/omni log/
 *
 * It strictly avoids private app internal data folders (/data/user/0/...) and
 * Android system directories (/Android/data/...).
 */
class OmniCrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashReportToOmniLog(thread, throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash report to omni log folder", e)
        } finally {
            // Forward to default system handler for clean termination
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashReportToOmniLog(thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "omnicam_crash_$timestamp.txt"

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTraceString = sw.toString()

        val reportContent = buildString {
            appendLine("==========================================================")
            appendLine("              OMNICAM VISION CRASH REPORT                 ")
            appendLine("==========================================================")
            appendLine("Date/Time:        ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US).format(Date())}")
            appendLine("Thread Name:      ${thread.name} (ID: ${thread.id})")
            appendLine("Exception Type:   ${throwable.javaClass.name}")
            appendLine("Message:          ${throwable.message}")
            appendLine("----------------------------------------------------------")
            appendLine("DEVICE & OS INFORMATION:")
            appendLine("Manufacturer:     ${Build.MANUFACTURER}")
            appendLine("Brand:            ${Build.BRAND}")
            appendLine("Model:            ${Build.MODEL}")
            appendLine("Device:           ${Build.DEVICE}")
            appendLine("Board:            ${Build.BOARD}")
            appendLine("Hardware:         ${Build.HARDWARE}")
            appendLine("Android Version:  ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Security Patch:   ${Build.VERSION.SECURITY_PATCH}")
            appendLine("App Package:      ${context.packageName}")
            appendLine("----------------------------------------------------------")
            appendLine("STACK TRACE:")
            appendLine(stackTraceString)
            appendLine("==========================================================")
            appendLine("END OF CRASH REPORT")
        }

        // Try direct file writes to SD Card / external storage omni log candidates
        val omniLogDir = getWritableOmniLogDirectory()
        if (omniLogDir != null) {
            try {
                val reportFile = File(omniLogDir, fileName)
                FileOutputStream(reportFile).use { fos ->
                    fos.write(reportContent.toByteArray(Charsets.UTF_8))
                    fos.flush()
                }
                Log.i(TAG, "Crash report successfully written to: ${reportFile.absolutePath}")
                return
            } catch (e: Exception) {
                Log.w(TAG, "Direct file write to ${omniLogDir.absolutePath} failed, attempting MediaStore fallback: ${e.message}")
            }
        }

        // Fallback for Scoped Storage: use MediaStore to save directly into public SD Card Download/omni log/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$OMNI_LOG_DIR_NAME")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(reportContent.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    }
                    Log.i(TAG, "Crash report successfully saved via MediaStore to: Download/$OMNI_LOG_DIR_NAME/$fileName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash report via MediaStore", e)
            }
        }
    }

    companion object {
        private const val TAG = "OmniCrashHandler"
        const val OMNI_LOG_DIR_NAME = "omni log"

        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (defaultHandler !is OmniCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(
                    OmniCrashHandler(context.applicationContext, defaultHandler)
                )
                Log.i(TAG, "OmniCrashHandler initialized. Target folder: SD card / $OMNI_LOG_DIR_NAME")
            }
        }

        private fun getCandidateDirs(): List<File> {
            return listOfNotNull(
                File(Environment.getExternalStorageDirectory(), OMNI_LOG_DIR_NAME),
                File("/sdcard", OMNI_LOG_DIR_NAME),
                File("/storage/emulated/0", OMNI_LOG_DIR_NAME),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), OMNI_LOG_DIR_NAME),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), OMNI_LOG_DIR_NAME)
            )
        }

        /**
         * Resolves the "omni log" directory on external storage/SD card.
         * Explicitly targets the SD card root (/sdcard/omni log or /storage/emulated/0/omni log),
         * and NEVER internal private files or /Android/data directories.
         */
        fun getOmniLogDirectory(): File? {
            return getWritableOmniLogDirectory() ?: File(Environment.getExternalStorageDirectory(), OMNI_LOG_DIR_NAME)
        }

        fun getWritableOmniLogDirectory(): File? {
            for (candidate in getCandidateDirs()) {
                try {
                    if (!candidate.exists()) {
                        candidate.mkdirs()
                    }
                    if (candidate.exists() && candidate.canWrite()) {
                        return candidate
                    }
                } catch (_: Exception) {
                }
            }
            return null
        }

        /**
         * Reads all saved crash log files in all external omni log folders.
         */
        fun getCrashReports(): List<File> {
            val results = mutableListOf<File>()
            for (dir in getCandidateDirs()) {
                try {
                    if (dir.exists() && dir.isDirectory) {
                        val files = dir.listFiles { file ->
                            file.isFile && (file.name.startsWith("omnicam_crash_") || file.name.endsWith(".txt"))
                        }
                        if (files != null) {
                            results.addAll(files)
                        }
                    }
                } catch (_: Exception) {
                }
            }
            return results.distinctBy { it.name }.sortedByDescending { it.lastModified() }
        }

        /**
         * Writes a diagnostic or manual crash log entry into omni log.
         */
        fun writeManualTestReport(context: Context, note: String): File? {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val fileName = "omnicam_crash_${timestamp}_diagnostic.txt"

            val content = buildString {
                appendLine("==========================================================")
                appendLine("          OMNICAM VISION CRASH REPORT (TEST/DIAGNOSTIC)    ")
                appendLine("==========================================================")
                appendLine("Date/Time:        ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US).format(Date())}")
                appendLine("Note:             $note")
                appendLine("App Package:      ${context.packageName}")
                appendLine("OS Release:       Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Device Model:     ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Target Folder:    SD Card / $OMNI_LOG_DIR_NAME")
                appendLine("Private/System:   NO (Folder is public SD card)")
                appendLine("----------------------------------------------------------")
                appendLine("STATUS: Verified write to SD card omni log folder.")
                appendLine("==========================================================")
            }

            val dir = getWritableOmniLogDirectory()
            if (dir != null) {
                try {
                    val file = File(dir, fileName)
                    FileOutputStream(file).use { fos ->
                        fos.write(content.toByteArray(Charsets.UTF_8))
                        fos.flush()
                    }
                    return file
                } catch (_: Exception) {
                }
            }

            // Fallback via MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$OMNI_LOG_DIR_NAME")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(content.toByteArray(Charsets.UTF_8))
                            os.flush()
                        }
                    }
                } catch (_: Exception) {
                }
            }
            return null
        }
    }
}

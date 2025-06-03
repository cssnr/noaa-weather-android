package org.cssnr.noaaweather.log

import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(private val logFile: File) : Timber.Tree() {

    private val logWriter = FileWriter(logFile, true)

    @Volatile
    var isLoggingEnabled: Boolean = false

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isLoggingEnabled) return

        val timestamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date())
        val level = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "D"
        }
        logWriter.write("$timestamp $level $message\n")
        logWriter.flush()
    }
}

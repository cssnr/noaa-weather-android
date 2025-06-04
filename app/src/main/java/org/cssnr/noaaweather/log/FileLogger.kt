package org.cssnr.noaaweather.log

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun Context.debugLog(message: String) {
    val debugLog =
        PreferenceManager.getDefaultSharedPreferences(this).getBoolean("enable_debug_logs", false)
    if (!debugLog) return
    val lock = ReentrantLock()
    val logFile = File(filesDir, "debug_log.txt")
    val logWriter = BufferedWriter(FileWriter(logFile, true))
    val timeStamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date())
    val logMessage = "$timeStamp - ${message}\n"
    Log.d("appendLog", "logMessage: $logMessage")
    lock.withLock {
        try {
            logWriter.write(logMessage)
            logWriter.flush()
            logWriter.close()
        } catch (e: IOException) {
            Log.e("appendLog", "IOException: $e")
        }
    }
}

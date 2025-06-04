package org.cssnr.noaaweather.log

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logLock = ReentrantLock()

fun Context.debugLog(message: String) {
    val debugLog = PreferenceManager.getDefaultSharedPreferences(this)
        .getBoolean("enable_debug_logs", false)
    if (!debugLog) return

    val logFile = File(filesDir, "debug_log.txt")
    val timeStamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date())
    val logMessage = "$timeStamp - $message\n"

    CoroutineScope(Dispatchers.IO).launch {
        logLock.withLock {
            try {
                FileWriter(logFile, true).use { fw ->
                    BufferedWriter(fw).use { bw ->
                        bw.write(logMessage)
                    }
                }
            } catch (e: IOException) {
                Log.e("debugLog", "IOException: $e")
            }
        }
    }
}

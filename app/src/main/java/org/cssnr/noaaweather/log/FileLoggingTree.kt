package org.cssnr.noaaweather.log

//import android.content.Context
//import android.util.Log
//import androidx.preference.PreferenceManager
//import java.io.BufferedWriter
//import java.io.File
//import java.io.FileWriter
//import java.io.IOException
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import java.util.concurrent.locks.ReentrantLock
//import kotlin.concurrent.withLock

//class FileLoggingTree(private val logFile: File) : Timber.Tree() {
//
//    private val lock = ReentrantLock()
//    private val logWriter: BufferedWriter = BufferedWriter(FileWriter(logFile, true))
//
//    @Volatile
//    var isLoggingEnabled: Boolean = false
//
//    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
//        if (!isLoggingEnabled) return
//        val timestamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date())
//        val level = when (priority) {
//            Log.VERBOSE -> "V"
//            Log.DEBUG -> "D"
//            Log.INFO -> "I"
//            Log.WARN -> "W"
//            Log.ERROR -> "E"
//            Log.ASSERT -> "A"
//            else -> "D"
//        }
//        val logLine = "$timestamp $level ${tag ?: "NO_TAG"}: $message\n"
//
//        lock.withLock {
//            try {
//                logWriter.write(logLine)
//                logWriter.flush()
//            } catch (e: IOException) {
//                Log.d("FileLoggingTree", "IOException: write: $e")
//            }
//        }
//    }
//
//    fun close() {
//        lock.withLock {
//            try {
//                logWriter.close()
//            } catch (e: IOException) {
//                Log.d("FileLoggingTree", "IOException: close: $e")
//            }
//        }
//    }
//}

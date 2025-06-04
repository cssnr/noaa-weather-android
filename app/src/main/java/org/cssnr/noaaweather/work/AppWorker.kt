package org.cssnr.noaaweather.work

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.log.FileLoggingTree
import org.cssnr.noaaweather.ui.stations.updateStation
import org.cssnr.noaaweather.widget.WidgetProvider
import timber.log.Timber
import java.io.File

class AppWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("AppWorker", "START: doWork")

        // Plant Timber
        val logFile = File(applicationContext.filesDir, "debug_log.txt")
        val fileLoggingTree = FileLoggingTree(logFile)
        Timber.plant(fileLoggingTree)

        // Update Current Conditions
        Log.d("AppWorker", "Update Current Conditions")
        try {
            val dao = StationDatabase.Companion.getInstance(applicationContext).stationDao()
            val station = dao.getActive()
            Log.d("AppWorker", "station: $station")
            if (station != null) {
                val result = applicationContext.updateStation(station.stationId)
                Timber.d("AppWorker: Update Station ${result?.stationId}")
            } else {
                Timber.d("AppWorker: No Active Station")
            }
        } catch (e: Exception) {
            Log.e("AppWorker", "Exception: $e")
            Timber.w("AppWorker: Exception: ${e.message}")
        }

        // Update Widget
        Log.d("AppWorker", "Update Widget")
        val componentName = ComponentName(applicationContext, WidgetProvider::class.java)
        Log.d("AppWorker", "componentName: $componentName")
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setClassName(
            applicationContext.packageName,
            "org.cssnr.noaaweather.widget.WidgetProvider"
        ).apply {
            val ids =
                AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(componentName)
            Log.d("AppWorker", "ids: $ids")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        Log.d("AppWorker", "sendBroadcast: $intent")
        applicationContext.sendBroadcast(intent)

        fileLoggingTree.close()
        Log.d("AppWorker", "DONE: doWork")
        return Result.success()
    }
}

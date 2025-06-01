package org.cssnr.noaaweather

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.cssnr.noaaweather.MainActivity.Companion.LOG_FILE
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.ui.WidgetProvider
import org.cssnr.noaaweather.ui.stations.updateStation

class AppWorker(val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("AppWorker", "START: doWork")

        // Update Current Conditions
        Log.d("AppWorker", "--- Update Current Conditions")
        try {
            val dao = StationDatabase.getInstance(applicationContext).stationDao()
            val station = dao.getActive()
            Log.d("AppWorker", "station: $station")
            if (station != null) {
                applicationContext.updateStation(station.stationId)
            }
            appContext.appendLog(LOG_FILE, "Update Success.")
        } catch (e: Exception) {
            Log.e("AppWorker", "Update Current Conditions: Exception: $e")
            appContext.appendLog(LOG_FILE, "Update Error: ${e.message}")
        }

        // Update Widget
        Log.d("AppWorker", "--- Update Widget")
        val componentName = ComponentName(applicationContext, WidgetProvider::class.java)
        Log.d("AppWorker", "componentName: $componentName")
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setClassName(
            applicationContext.packageName,
            "org.cssnr.noaaweather.WidgetProvider"
        ).apply {
            val ids =
                AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(componentName)
            Log.d("AppWorker", "ids: $ids")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        Log.d("AppWorker", "sendBroadcast: $intent")
        applicationContext.sendBroadcast(intent)

        Log.d("AppWorker", "DONE: doWork")
        return Result.success()
    }
}

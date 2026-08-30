package org.cssnr.noaaweather.work

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.log.debugLog
import org.cssnr.noaaweather.ui.stations.StationUpdateStatus
import org.cssnr.noaaweather.ui.stations.updateStationResult
import org.cssnr.noaaweather.widget.WidgetProvider

class AppWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("AppWorker", "START: doWork")

        // Update Current Conditions
        Log.d("AppWorker", "Update Current Conditions")
        try {
            val dao = StationDatabase.getInstance(applicationContext).stationDao()
            val station = dao.getActive()
            Log.d("AppWorker", "station: $station")
            if (station != null) {
                val result = applicationContext.updateStationResult(station.stationId)
                val status = when (result.status) {
                    StationUpdateStatus.UPDATED -> "Updated"
                    StationUpdateStatus.UNCHANGED -> "Unchanged"
                    StationUpdateStatus.FAILED -> "Update Failed"
                    StationUpdateStatus.NOT_FOUND -> "Not Found"
                }
                applicationContext.debugLog(
                    "AppWorker: $status Station ${result.station?.stationId ?: station.stationId}"
                )
            } else {
                applicationContext.debugLog("AppWorker: No Active Station")
            }
        } catch (e: Exception) {
            Log.e("AppWorker", "Exception: $e")
            applicationContext.debugLog("AppWorker: Exception: ${e.message}")
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

        Log.d("AppWorker", "DONE: doWork")
        return Result.success()
    }
}

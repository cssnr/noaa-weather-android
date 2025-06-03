package org.cssnr.noaaweather

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.ui.stations.updateStation
import org.cssnr.noaaweather.widget.WidgetProvider
import org.cssnr.noaaweather.widget.WidgetUpdater

class AppWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("AppWorker", "START: doWork")
        applicationContext.appendLog("Executing Background Task.")

        // Update Current Conditions
        Log.d("AppWorker", "Update Current Conditions")
        try {
            val dao = StationDatabase.getInstance(applicationContext).stationDao()
            val station = dao.getActive()
            Log.d("AppWorker", "station: $station")
            if (station != null) {
                applicationContext.updateStation(station.stationId)
            }
        } catch (e: Exception) {
            Log.e("AppWorker", "Exception: $e")
            applicationContext.appendLog("Worker Error: ${e.message}")
        }


        // Update Widget
        Log.d("AppWorker", "WidgetUpdater.updateWidget")
        val manager = AppWidgetManager.getInstance(applicationContext)
        val widgetIds =
            manager.getAppWidgetIds(ComponentName(applicationContext, WidgetProvider::class.java))
        WidgetUpdater.updateWidget(applicationContext, manager, widgetIds)

        Log.d("AppWorker", "DONE: doWork")
        return Result.success()
    }
}

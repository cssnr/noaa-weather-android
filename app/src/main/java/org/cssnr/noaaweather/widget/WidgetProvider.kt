package org.cssnr.noaaweather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.cssnr.noaaweather.db.StationDao
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.ui.stations.updateStation

class WidgetProvider : AppWidgetProvider() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("Widget[onReceive]", "intent: $intent")

        if (intent.action == "org.cssnr.noaaweather.REFRESH_WIDGET") {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return
            }
            Log.d("Widget[onReceive]", "GlobalScope.launch: START")
            GlobalScope.launch(Dispatchers.IO) {
                val dao: StationDao =
                    StationDatabase.getInstance(context.applicationContext).stationDao()
                val station = dao.getActive()
                Log.d("Widget[onReceive]", "station: $station")
                if (station != null) {
                    val current = context.updateStation(station.stationId)
                    Log.d("Widget[onReceive]", "current: $current")
                }
                val appWidgetManager = AppWidgetManager.getInstance(context)
                onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
                Log.d("Widget[onReceive]", "GlobalScope.launch: DONE")
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("WidgetProvider", "onUpdate")
        WidgetUpdater.updateWidget(context, appWidgetManager, appWidgetIds)
    }
}

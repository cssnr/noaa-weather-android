package org.cssnr.noaaweather.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.cssnr.noaaweather.MainActivity
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.db.StationDao
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.ui.stations.getCurrentConditions

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
                    val current = context.getCurrentConditions(station.stationId)
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
        Log.d("Widget[onUpdate]", "appWidgetIds: $appWidgetIds")

        appWidgetIds.forEach { appWidgetId ->
            Log.d("Widget[onUpdate]", "appWidgetId: $appWidgetId")

            // Widget Root
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val pendingIntent0: PendingIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply { action = Intent.ACTION_MAIN },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent0)

            // Refresh
            val intent1 = Intent(context, WidgetProvider::class.java).apply {
                action = "org.cssnr.noaaweather.REFRESH_WIDGET"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent1 = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent1,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, pendingIntent1)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            GlobalScope.launch(Dispatchers.IO) {
                val dao: StationDao =
                    StationDatabase.getInstance(context.applicationContext).stationDao()
                val station = dao.getActive()
                Log.d("Widget[onUpdate]", "station: $station")
                Log.d("Widget[onUpdate]", "name: ${station?.name}")
                views.setTextViewText(R.id.station_name, station?.name ?: "No Stations Found")

                Log.d("Widget[onUpdate]", "station.temperature: ${station?.temperature}")
                val temperature =
                    context.getString(R.string.format_temp_c, station?.temperature, "°C")
                Log.d("Widget[onUpdate]", "temperature: $temperature")
                views.setTextViewText(R.id.station_temperature, temperature)

                Log.d("Widget[onUpdate]", "station.relativeHumidity: ${station?.relativeHumidity}")
                val humidity = context.getString(R.string.format_percent, station?.relativeHumidity)
                Log.d("Widget[onUpdate]", "humidity: $humidity")
                views.setTextViewText(R.id.station_humidity, humidity)

                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d("Widget[onUpdate]", "updateAppWidget: DONE")
            }
            Log.d("Widget[onUpdate]", "appWidgetIds.forEach: DONE")
        }
        Log.d("Widget[onUpdate]", "onUpdate: DONE")
    }
}

package org.cssnr.noaaweather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Color
import android.text.format.DateFormat
import android.util.Log
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.cssnr.noaaweather.MainActivity
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.db.StationDao
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.ui.home.getTemp
import org.cssnr.noaaweather.ui.home.getValue
import java.time.ZonedDateTime
import java.util.Date

object WidgetUpdater {

    @OptIn(DelicateCoroutinesApi::class)
    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.i("WidgetUpdater", "BEGIN - appWidgetIds: $appWidgetIds")
        Log.d("Widget[onUpdate]", "appWidgetIds: $appWidgetIds")

        val preferences = context.getSharedPreferences("org.cssnr.noaaweather", MODE_PRIVATE)
        val tempUnit = preferences.getString("temp_unit", null) ?: "C"
        Log.d("Widget[onUpdate]", "tempUnit: $tempUnit")
        val bgColor = preferences.getString("widget_bg_color", null) ?: "black"
        Log.i("Widget[onUpdate]", "bgColor: $bgColor")
        val textColor = preferences.getString("widget_text_color", null) ?: "white"
        Log.i("Widget[onUpdate]", "textColor: $textColor")
        val bgOpacity = preferences.getInt("widget_bg_opacity", 35)
        Log.d("WidgetUpdater", "bgOpacity: $bgOpacity")
        val workInterval = preferences.getString("work_interval", null) ?: "0"
        Log.d("WidgetUpdater", "workInterval: $workInterval")

        val colorMap = mapOf(
            "white" to Color.WHITE,
            "black" to Color.BLACK,
            "blue1" to "#0085ca".toColorInt(),
            "blue2" to "#003087".toColorInt(),
        )

        val selectedBgColor = colorMap[bgColor] ?: Color.BLACK
        Log.d("Widget[onUpdate]", "selectedBgColor: $selectedBgColor")
        val selectedTextColor = colorMap[textColor] ?: Color.WHITE
        Log.d("Widget[onUpdate]", "selectedTextColor: $selectedTextColor")

        val opacityPercent = bgOpacity
        val alpha = (opacityPercent * 255 / 100).coerceIn(1, 255)
        val finalBgColor = ColorUtils.setAlphaComponent(selectedBgColor, alpha)
        Log.d("WidgetUpdater", "finalBgColor: $finalBgColor")

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

            // Set Colors
            views.setInt(R.id.widget_root, "setBackgroundColor", finalBgColor)
            views.setTextColor(R.id.station_name, selectedTextColor)
            views.setTextColor(R.id.station_temperature, selectedTextColor)
            views.setTextColor(R.id.station_humidity, selectedTextColor)
            views.setTextColor(R.id.update_time, selectedTextColor)
            views.setTextColor(R.id.update_interval, selectedTextColor)
            views.setInt(R.id.temperature_icon, "setColorFilter", selectedTextColor)
            views.setInt(R.id.humidity_icon, "setColorFilter", selectedTextColor)
            views.setInt(R.id.refresh_icon, "setColorFilter", selectedTextColor)
            views.setInt(R.id.update_interval_icon, "setColorFilter", selectedTextColor)

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

            // Room Data
            GlobalScope.launch(Dispatchers.IO) {
                val dao: StationDao =
                    StationDatabase.getInstance(context.applicationContext).stationDao()
                val station = dao.getActive()
                Log.d("Widget[onUpdate]", "station: $station")
                Log.d("Widget[onUpdate]", "name: ${station?.name}")
                views.setTextViewText(R.id.station_name, station?.name ?: "No Stations Found")

                Log.d("Widget[onUpdate]", "station.temperature: ${station?.temperature}")
                val temperature = context.getTemp(station?.temperature, tempUnit)
                Log.d("Widget[onUpdate]", "temperature: $temperature")
                views.setTextViewText(R.id.station_temperature, temperature)

                Log.d("Widget[onUpdate]", "station.relativeHumidity: ${station?.relativeHumidity}")
                val humidity = context.getValue(R.string.format_percent, station?.relativeHumidity)
                Log.d("Widget[onUpdate]", "humidity: $humidity")
                views.setTextViewText(R.id.station_humidity, humidity)

                // Interval
                val interval = if (workInterval != "0") workInterval else "Off"
                Log.d("WidgetUpdater", "interval: $interval")
                Log.i("WidgetUpdater", "intervalintervalintervalintervalinterval: $interval")
                views.setTextViewText(R.id.update_interval, interval)

                ////val formatted = DateFormat.getTimeFormat(context).format(Date())
                //val zonedDateTime = ZonedDateTime.parse(station?.timestamp)
                //val instant = zonedDateTime.toInstant()
                //val localDate = Date.from(instant)
                //val formatted = DateFormat.getTimeFormat(context).format(localDate)
                //Log.d("Widget[onUpdate]", "formatted: $formatted")
                //views.setTextViewText(R.id.update_time, formatted)

                station?.timestamp?.let {
                    val zonedDateTime = ZonedDateTime.parse(it)
                    val instant = zonedDateTime.toInstant()
                    val localDate = Date.from(instant)
                    val formatted = DateFormat.getTimeFormat(context).format(localDate)
                    Log.d("Widget[onUpdate]", "formatted: $formatted")
                    views.setTextViewText(R.id.update_time, formatted)
                }

                // TODO: Add Interval
                //val interval = if (workInterval != "0") workInterval else "Off"
                //Log.d("WidgetUpdater", "interval: $interval")
                //views.setTextViewText(R.id.update_interval, interval)

                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d("Widget[onUpdate]", "updateAppWidget: DONE")
            }
            Log.d("Widget[onUpdate]", "appWidgetIds.forEach: DONE")
        }
        Log.i("WidgetUpdater", "END - all done")
    }
}

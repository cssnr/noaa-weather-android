package org.cssnr.noaaweather.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.cssnr.noaaweather.R

class WidgetConfiguration : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    companion object {
        const val LOG_TAG = "WidgetConfiguration"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_configure)

        setResult(RESULT_CANCELED)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish()

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val bgColor = preferences.getString("widget_bg_color", null) ?: "black"
        Log.i(LOG_TAG, "bgColor: $bgColor")
        val textColor = preferences.getString("widget_text_color", null) ?: "white"
        Log.i(LOG_TAG, "textColor: $textColor")
        val bgOpacity = preferences.getInt("widget_bg_opacity", 35)
        Log.i("WidgetConfiguration", "bgOpacity: $bgOpacity")

        val bgOpacityText = findViewById<TextView>(R.id.bg_opacity_percent)
        bgOpacityText.text = getString(R.string.background_opacity, bgOpacity)

        //val w = "option_white"
        //val resId = resources.getIdentifier(w, "id", packageName)
        //val whiteButton = findViewById<RadioButton>(resId)
        //Log.i(LOG_TAG, "whiteButton: $whiteButton")

        val bgColorId = mapOf(
            "white" to R.id.option_white,
            "black" to R.id.option_black,
            "blue1" to R.id.option_light_blue,
            "blue2" to R.id.option_dark_blue,
        )
        val textColorId = mapOf(
            "white" to R.id.text_white,
            "black" to R.id.text_black,
            "blue1" to R.id.text_light_blue,
            "blue2" to R.id.text_dark_blue,
        )

        val backgroundOptions = findViewById<RadioGroup>(R.id.background_options)
        val textOptions = findViewById<RadioGroup>(R.id.text_options)
        val confirmButton = findViewById<Button>(R.id.confirm_button)

        val bgSelected = bgColorId[bgColor]
        if (bgSelected != null) backgroundOptions.check(bgSelected)
        val textSelected = textColorId[textColor]
        if (textSelected != null) textOptions.check(textSelected)

        val seekBar = findViewById<SeekBar>(R.id.opacity_percent)
        seekBar.progress = bgOpacity
        //seekBar.progress = ((bgOpacity + 2) / 5) * 5
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && seekBar != null) {
                    val stepped = ((progress + 2) / 5) * 5
                    seekBar.progress = stepped
                    bgOpacityText.text = getString(R.string.background_opacity, stepped)
                    Log.d("onProgressChanged", "stepped: $stepped")
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.d("onProgressChanged", "START")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Log.d("onProgressChanged", "STOP")
            }
        })

        confirmButton.setOnClickListener {
            Log.i(LOG_TAG, "backgroundOptions: ${backgroundOptions.checkedRadioButtonId}")
            val selectedBgColor = when (backgroundOptions.checkedRadioButtonId) {
                R.id.option_white -> "white"
                R.id.option_black -> "black"
                R.id.option_light_blue -> "blue1"
                R.id.option_dark_blue -> "blue2"
                else -> "black"
            }
            Log.i(LOG_TAG, "selectedBgColor: $selectedBgColor")

            Log.i(LOG_TAG, "textOptions: ${textOptions.checkedRadioButtonId}")
            val selectedTextColor = when (textOptions.checkedRadioButtonId) {
                R.id.text_white -> "white"
                R.id.text_black -> "black"
                R.id.text_light_blue -> "blue1"
                R.id.text_dark_blue -> "blue2"
                else -> "white"
            }
            Log.i(LOG_TAG, "selectedTextColor: $selectedTextColor")

            Log.i("WidgetConfiguration", "seekBar.progress: ${seekBar.progress}")

            preferences.edit {
                putString("widget_bg_color", selectedBgColor)
                putString("widget_text_color", selectedTextColor)
                putInt("widget_bg_opacity", seekBar.progress)
            }

            val updateIntent = Intent(
                AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                null,
                this,
                WidgetProvider::class.java
            )
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            sendBroadcast(updateIntent)

            val result = Intent()
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, result)
            finish()
        }
    }
}

package org.cssnr.noaaweather.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.R

class WidgetConfigurationActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_configure)

        val root = findViewById<LinearLayout>(R.id.config_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            v.setPadding(24, 24, 24, 24)
            insets
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

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

        val preferences = getSharedPreferences("org.cssnr.noaaweather", MODE_PRIVATE)
        val bgColor = preferences.getString("widget_bg_color", null) ?: "transparent"
        Log.i(LOG_TAG, "bgColor: $bgColor")
        val textColor = preferences.getString("widget_text_color", null) ?: "white"
        Log.i(LOG_TAG, "textColor: $textColor")

        //val w = "option_white"
        //val resId = resources.getIdentifier(w, "id", packageName)
        //val whiteButton = findViewById<RadioButton>(resId)
        //Log.i(LOG_TAG, "whiteButton: $whiteButton")

        val bgColorId = mapOf(
            "white" to R.id.option_white,
            "black" to R.id.option_black,
            "transparent" to R.id.option_transparent
        )
        val textColorId = mapOf(
            "white" to R.id.text_white,
            "black" to R.id.text_black,
        )

        val backgroundOptions = findViewById<RadioGroup>(R.id.background_options)
        val textOptions = findViewById<RadioGroup>(R.id.text_options)
        val confirmButton = findViewById<Button>(R.id.confirm_button)

        val bgSelected = bgColorId[bgColor]
        if (bgSelected != null) backgroundOptions.check(bgSelected)
        val textSelected = textColorId[textColor]
        if (textSelected != null) textOptions.check(textSelected)

        confirmButton.setOnClickListener {
            Log.i(LOG_TAG, "backgroundOptions: ${backgroundOptions.checkedRadioButtonId}")
            val selectedBgColor = when (backgroundOptions.checkedRadioButtonId) {
                R.id.option_white -> "white"
                R.id.option_black -> "black"
                R.id.option_transparent -> "transparent"
                else -> "transparent"
            }
            Log.i(LOG_TAG, "selectedBgColor: $selectedBgColor")

            Log.i(LOG_TAG, "textOptions: ${textOptions.checkedRadioButtonId}")
            val selectedTextColor = when (textOptions.checkedRadioButtonId) {
                R.id.text_white -> "white"
                R.id.text_black -> "black"
                else -> "white"
            }
            Log.i(LOG_TAG, "selectedTextColor: $selectedTextColor")

            preferences.edit {
                putString("widget_bg_color", selectedBgColor)
                putString("widget_text_color", selectedTextColor)
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

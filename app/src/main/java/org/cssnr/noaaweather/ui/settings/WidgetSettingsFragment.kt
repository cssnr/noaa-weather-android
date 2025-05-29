package org.cssnr.noaaweather.ui.settings

import android.os.Bundle
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.R

class WidgetSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(LOG_TAG, "onCreatePreferences: rootKey: $rootKey")

        preferenceManager.sharedPreferencesName = "org.cssnr.noaaweather"
        setPreferencesFromResource(R.xml.preferences_widget, rootKey)

        // Text Color
        val textColor = findPreference<ListPreference>("widget_text_color")
        Log.d(LOG_TAG, "textColor: $textColor")
        textColor?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

        // BG Color
        val bgColor = findPreference<ListPreference>("widget_bg_color")
        Log.d(LOG_TAG, "bgColor: $bgColor")
        bgColor?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
    }
}

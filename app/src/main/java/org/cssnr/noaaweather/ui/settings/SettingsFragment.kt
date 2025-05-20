package org.cssnr.noaaweather.ui.settings

import android.os.Bundle
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.cssnr.noaaweather.AppWorker
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.R
import java.util.concurrent.TimeUnit

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(LOG_TAG, "onCreatePreferences: rootKey: $rootKey")

        preferenceManager.sharedPreferencesName = "org.cssnr.noaaweather"
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val tempUnits = findPreference<ListPreference>("temp_unit")
        Log.d(LOG_TAG, "tempUnits: $tempUnits")
        tempUnits?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

        val workInterval = findPreference<ListPreference>("work_interval")
        Log.d(LOG_TAG, "workInterval: $workInterval")
        workInterval?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        workInterval?.setOnPreferenceChangeListener { _, rawValue ->
            Log.d(LOG_TAG, "Current Value: ${workInterval.value}")
            val newValue = rawValue.toString()
            Log.d(LOG_TAG, "New Value: $newValue")
            if (workInterval.value != newValue) {
                Log.i(LOG_TAG, "Rescheduling Work Request")
                val interval = newValue.toLongOrNull()
                Log.d(LOG_TAG, "interval: $interval")
                if (newValue != "0" && interval != null) {
                    val newRequest =
                        PeriodicWorkRequestBuilder<AppWorker>(interval, TimeUnit.MINUTES)
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiresBatteryNotLow(true)
                                    .setRequiresCharging(false)
                                    .setRequiresDeviceIdle(false)
                                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                    .build()
                            )
                            .build()
                    WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                        "app_worker",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        newRequest
                    )
                } else {
                    if (interval == null) {
                        Log.e(LOG_TAG, "Interval is null: $interval")
                    }
                    Log.i(LOG_TAG, "CANCEL WORK: app_worker")
                    WorkManager.getInstance(requireContext()).cancelUniqueWork("app_worker")
                }
                Log.d(LOG_TAG, "true: ACCEPTED")
                true
            } else {
                Log.d(LOG_TAG, "false: REJECTED")
                false
            }
        }
    }
}

package org.cssnr.noaaweather.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import androidx.preference.ListPreference
import androidx.preference.Preference
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

    @SuppressLint("BatteryLife")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(LOG_TAG, "onCreatePreferences: rootKey: $rootKey")

        preferenceManager.sharedPreferencesName = "org.cssnr.noaaweather"
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Temperature Unit
        val tempUnits = findPreference<ListPreference>("temp_unit")
        Log.d(LOG_TAG, "tempUnits: $tempUnits")
        tempUnits?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

        // Work Interval
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

        // Background Restriction
        val packageName = requireContext().packageName
        Log.i(LOG_TAG, "packageName: $packageName")
        val pm = requireContext().getSystemService(PowerManager::class.java)

        val batteryRestrictedButton = findPreference<Preference>("battery_unrestricted")

        fun checkBackground(): Boolean {
            val isIgnoring = pm.isIgnoringBatteryOptimizations(packageName)
            Log.i(LOG_TAG, "isIgnoring: $isIgnoring")
            if (isIgnoring) {
                Log.i(LOG_TAG, "DISABLING BACKGROUND BUTTON")
                batteryRestrictedButton?.setSummary("Permission Already Granted")
                batteryRestrictedButton?.isEnabled = false
            }
            return isIgnoring
        }

        checkBackground()

        batteryRestrictedButton?.setOnPreferenceClickListener {
            Log.d(LOG_TAG, "batteryRestrictedButton?.setOnPreferenceClickListener")
            if (!checkBackground()) {
                val uri = "package:$packageName".toUri()
                Log.d(LOG_TAG, "uri: $uri")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = uri
                }
                Log.d(LOG_TAG, "intent: $intent")
                startActivity(intent)
            }
            false
        }
    }
}

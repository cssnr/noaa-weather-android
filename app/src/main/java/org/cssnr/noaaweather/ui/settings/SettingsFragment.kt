package org.cssnr.noaaweather.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.api.FeedbackApi
import org.cssnr.noaaweather.work.APP_WORKER_CONSTRAINTS
import org.cssnr.noaaweather.work.AppWorker
import java.util.concurrent.TimeUnit

const val LOG_TAG = "SettingsFragment"

class SettingsFragment : PreferenceFragmentCompat() {

    var checkPerms = false

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    @SuppressLint("BatteryLife")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d("SettingsFragment", "rootKey: $rootKey")
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val ctx = requireContext()

        // Temperature Unit
        val tempUnit = findPreference<ListPreference>("temp_unit")
        Log.d(LOG_TAG, "tempUnit: $tempUnit")
        tempUnit?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()


        // Notifications - Default Channel - TESTING ONLY
        val requestPermissionLauncher =
            registerForActivityResult(RequestPermission()) { result ->
                Log.d(LOG_TAG, "result: $result")
            }
        val enableNotifications = findPreference<SwitchPreferenceCompat>("notifications_enabled")
        Log.d(LOG_TAG, "enableNotifications: $enableNotifications")
        enableNotifications?.setOnPreferenceChangeListener { _, newValue ->
            Log.d(LOG_TAG, "notifications_enabled: $newValue")
            if (newValue as Boolean && !ctx.areNotificationsEnabled()) {
                checkPerms = true
                ctx.requestPerms(requestPermissionLauncher, newValue, "default_channel_id")
                false
            } else {
                true
            }
        }
        findPreference<Preference>("default_channel_id_status")?.setOnPreferenceClickListener {
            ctx.launchNotificationSettings("default_channel_id")
            false
        }
        //// Manage Notifications
        //val manageNotifications = findPreference<Preference>("manage_notifications")
        //manageNotifications?.setOnPreferenceClickListener {
        //    Log.d(LOG_TAG, "CLICK - manage_notifications")
        //    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
        //        putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        //        putExtra(Settings.EXTRA_CHANNEL_ID, "default_channel_id")
        //    }
        //    startActivity(intent)
        //    false
        //}


        // Update Interval
        val workInterval = findPreference<ListPreference>("work_interval")
        workInterval?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        workInterval?.setOnPreferenceChangeListener { _, newValue ->
            Log.d("work_interval", "newValue: $newValue")
            ctx.updateWorkManager(workInterval, newValue)
        }

        // Background Restriction
        Log.i(LOG_TAG, "packageName: ${ctx.packageName}")
        val pm = ctx.getSystemService(PowerManager::class.java)
        val batteryRestrictedButton = findPreference<Preference>("battery_unrestricted")

        fun checkBackground(): Boolean {
            val isIgnoring = pm.isIgnoringBatteryOptimizations(ctx.packageName)
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
                val uri = "package:${ctx.packageName}".toUri()
                Log.d(LOG_TAG, "uri: $uri")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = uri
                }
                Log.d(LOG_TAG, "intent: $intent")
                startActivity(intent)
            }
            false
        }

        // Widget Settings
        findPreference<Preference>("open_widget_settings")?.setOnPreferenceClickListener {
            Log.d("open_widget_settings", "setOnPreferenceClickListener")
            findNavController().navigate(R.id.nav_action_settings_widget)
            false
        }

        // Send Feedback
        val sendFeedback = findPreference<Preference>("send_feedback")
        sendFeedback?.setOnPreferenceClickListener {
            Log.d("sendFeedback", "setOnPreferenceClickListener")
            ctx.showFeedbackDialog()
            false
        }

        // Show App Info
        findPreference<Preference>("app_info")?.setOnPreferenceClickListener {
            Log.d("app_info", "showAppInfoDialog")
            ctx.showAppInfoDialog()
            false
        }

        // Open App Settings
        findPreference<Preference>("android_settings")?.setOnPreferenceClickListener {
            Log.d("android_settings", "setOnPreferenceClickListener")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
            }
            startActivity(intent)
            false
        }

        // Debugging
        val enableDebugLogs = findPreference<SwitchPreferenceCompat>("enable_debug_logs")
        val viewDebugLogs = findPreference<Preference>("view_debug_logs")
        enableDebugLogs?.setOnPreferenceChangeListener { _, newValue ->
            Log.d("enableDebugLogs", "enable_debug_logs: $newValue")
            val value = newValue as? Boolean == true
            Log.d("enableDebugLogs", "Boolean value: $value")
            viewDebugLogs?.isEnabled = value
            true
        }
        viewDebugLogs?.isEnabled = enableDebugLogs?.isChecked == true
        viewDebugLogs?.setOnPreferenceClickListener {
            Log.d("viewDebugLogs", "setOnPreferenceClickListener")
            findNavController().navigate(R.id.nav_action_settings_debug)
            false
        }
    }

    fun Context.updateWorkManager(listPref: ListPreference, newValue: Any): Boolean {
        Log.d("updateWorkManager", "listPref: ${listPref.value} - newValue: $newValue")
        val value = newValue as? String
        Log.d("updateWorkManager", "String value: $value")
        if (value.isNullOrEmpty()) {
            Log.w("updateWorkManager", "NULL OR EMPTY - false")
            return false
        } else if (listPref.value == value) {
            Log.i("updateWorkManager", "NO CHANGE - false")
            return false
        } else {
            Log.i("updateWorkManager", "RESCHEDULING WORK - true")
            val interval = value.toLongOrNull()
            Log.i("updateWorkManager", "interval: $interval")
            if (interval == null || interval == 0L) {
                Log.i("updateWorkManager", "DISABLING WORK")
                WorkManager.getInstance(this).cancelUniqueWork("app_worker")
                return true
            } else {
                val newRequest =
                    PeriodicWorkRequestBuilder<AppWorker>(interval, TimeUnit.MINUTES)
                        .setInitialDelay(1, TimeUnit.MINUTES)
                        .setConstraints(APP_WORKER_CONSTRAINTS)
                        .build()
                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "app_worker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    newRequest
                )
                return true
            }
        }
    }

    fun Context.showFeedbackDialog() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_feedback, null)
        val input = view.findViewById<EditText>(R.id.feedback_input)
        val link = view.findViewById<TextView>(R.id.github_link)

        link.paint?.isUnderlineText = true
        link.setOnClickListener {
            Log.d(LOG_TAG, "link.tag: ${link.tag}")
            startActivity(Intent(Intent.ACTION_VIEW, link.tag.toString().toUri()))
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send", null)
            .create()

        dialog.setOnShowListener {
            val sendButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            sendButton.setOnClickListener {
                sendButton.isEnabled = false
                val message = input.text.toString().trim()
                Log.d("showFeedbackDialog", "message: $message")
                if (message.isNotEmpty()) {
                    val api = FeedbackApi(this)
                    lifecycleScope.launch {
                        val response = withContext(Dispatchers.IO) { api.sendFeedback(message) }
                        Log.d("showFeedbackDialog", "response: $response")
                        val msg = if (response.isSuccessful) {
                            findPreference<Preference>("send_feedback")?.isEnabled = false
                            dialog.dismiss()
                            "Feedback Sent. Thank You!"
                        } else {
                            sendButton.isEnabled = true
                            //val params = Bundle().apply {
                            //    putString("message", response.message())
                            //    putString("code", response.code().toString())
                            //}
                            //Firebase.analytics.logEvent("feedback_failed", params)
                            "Error: ${response.code()}"
                        }
                        Log.d("showFeedbackDialog", "msg: $msg")
                        Toast.makeText(this@showFeedbackDialog, msg, Toast.LENGTH_LONG).show()
                    }
                } else {
                    sendButton.isEnabled = true
                    input.error = "Feedback is Required"
                }
            }

            input.requestFocus()
        }

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Send") { _, _ -> }
        dialog.show()
    }

    fun Context.showAppInfoDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_app_info, null)
        val appId = view.findViewById<TextView>(R.id.app_identifier)
        val appVersion = view.findViewById<TextView>(R.id.app_version)
        val sourceLink = view.findViewById<TextView>(R.id.source_link)
        val websiteLink = view.findViewById<TextView>(R.id.website_link)

        appId.text = packageName
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val formattedVersion = getString(
            R.string.version_code_string,
            packageInfo.versionName,
            packageInfo.versionCode.toString()
        )
        Log.d("showAppInfoDialog", "formattedVersion: $formattedVersion")
        appVersion.text = formattedVersion

        sourceLink.paint?.isUnderlineText = true
        sourceLink.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, sourceLink.tag.toString().toUri()))
        }
        websiteLink.paint?.isUnderlineText = true
        websiteLink.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, websiteLink.tag.toString().toUri()))
        }

        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Close", null)
            .create()
            .show()
    }

    override fun onResume() {
        Log.d(LOG_TAG, "ON RESUME - checkPerms: $checkPerms")
        super.onResume()
        val notificationsEnabled = preferences.getBoolean("notifications_enabled", false)
        Log.d(LOG_TAG, "notifications_enabled: $notificationsEnabled")
        if (notificationsEnabled && context?.areNotificationsEnabled() == false) {
            checkPerms = true
        }
        Log.d(LOG_TAG, "checkPerms: $checkPerms")

        if (checkPerms) {
            checkPerms = false
            val enableNotifications =
                findPreference<SwitchPreferenceCompat>("notifications_enabled")
            enableNotifications?.isChecked = context?.areNotificationsEnabled() == true
        }
        //val notificationsEnabled = context?.areNotificationsEnabled() == true
        //Log.i(LOG_TAG, "notificationsEnabled: $notificationsEnabled")
        //enableNotifications?.isChecked = notificationsEnabled
        context?.updateNotificationStatus()
    }

    private fun Context.updateNotificationStatus() {
        val notificationManager = NotificationManagerCompat.from(this)
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        Log.i(LOG_TAG, "areNotificationsEnabled: $areNotificationsEnabled")
        for (channel in notificationManager.notificationChannels) {
            Log.d(LOG_TAG, "pref key: ${channel.id}_status")
            val pref = findPreference<Preference>("${channel.id}_status")
            Log.d(LOG_TAG, "pref: $pref")
            if (pref == null) continue

            val alertsEnabled =
                channel.importance != NotificationManager.IMPORTANCE_NONE && areNotificationsEnabled
            Log.i(LOG_TAG, "alertsEnabled: $alertsEnabled")

            val playsSound =
                channel.sound != null && channel.importance >= NotificationManager.IMPORTANCE_DEFAULT
            Log.i(LOG_TAG, "playsSound: $playsSound")

            val hasVibration =
                channel.shouldVibrate() && channel.importance >= NotificationManager.IMPORTANCE_DEFAULT
            Log.i(LOG_TAG, "hasVibration: $hasVibration")

            val isSilent = channel.importance <= NotificationManager.IMPORTANCE_LOW &&
                    !playsSound &&
                    !hasVibration
            Log.i(LOG_TAG, "isSilent: $isSilent")

            val statuses = mutableSetOf<String>()
            if (!alertsEnabled) {
                statuses.add("Blocked")
            } else {
                if (isSilent) {
                    statuses.add("Silent")
                }
                if (playsSound) {
                    statuses.add("Sound")
                }
                if (hasVibration) {
                    statuses.add("Vibrate")
                }
            }
            if (statuses.isEmpty()) {
                statuses.add("No Sound or Vibration")
            }
            val result = statuses.joinToString(",")
            pref.summary = "Status: $result"
        }
    }
}

internal fun Context.launchNotificationSettings(channelId: String = "default_channel_id") {
    val notificationManager = NotificationManagerCompat.from(this)
    val globalEnabled = notificationManager.areNotificationsEnabled()
    Log.i("areNotificationsEnabled", "globalEnabled: $globalEnabled")
    val intent = if (globalEnabled) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        }
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
    }
    startActivity(intent)
}

fun Context.requestPerms(
    requestPermissionLauncher: ActivityResultLauncher<String>,
    newValue: Boolean,
    channelId: String = "default_channel_id",
) {
    if (!newValue) {
        launchNotificationSettings(channelId)
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val perm = Manifest.permission.POST_NOTIFICATIONS
        when {
            ContextCompat.checkSelfPermission(this, perm) ==
                    PackageManager.PERMISSION_GRANTED -> {
                Log.d("requestPerms", "1 - Permission Already Granted")
                launchNotificationSettings(channelId)
            }

            ActivityCompat.shouldShowRequestPermissionRationale(this as Activity, perm) -> {
                Log.d("requestPerms", "2 - shouldShowRequestPermissionRationale")
                launchNotificationSettings(channelId)
            }

            else -> {
                Log.d("requestPerms", "3 - requestPermissionLauncher")
                requestPermissionLauncher.launch(perm)
            }
        }
    } else {
        Log.d("requestPerms", "4 - PRE TIRAMISU")
        launchNotificationSettings(channelId)
    }
}

fun Context.areNotificationsEnabled(): Boolean {
    val notificationManager = NotificationManagerCompat.from(this)
    return when {
        notificationManager.areNotificationsEnabled().not() -> false
        else -> {
            notificationManager.notificationChannels.firstOrNull { channel ->
                channel.importance == NotificationManager.IMPORTANCE_NONE
            } == null
        }
    }
}

package org.cssnr.noaaweather.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.noaaweather.AppWorker
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.api.FeedbackApi
import java.util.concurrent.TimeUnit

class SettingsFragment : PreferenceFragmentCompat() {

    @SuppressLint("BatteryLife")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d("SettingsFragment", "rootKey: $rootKey - name: org.cssnr.noaaweather")
        preferenceManager.sharedPreferencesName = "org.cssnr.noaaweather"
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val ctx = requireContext()

        // Temperature Unit
        val tempUnit = findPreference<ListPreference>("temp_unit")
        Log.d(LOG_TAG, "tempUnit: $tempUnit")
        tempUnit?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

        // Background Update Interval
        val workInterval = findPreference<ListPreference>("work_interval")
        workInterval?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        workInterval?.setOnPreferenceChangeListener { _, newValue ->
            Log.d("work_interval", "newValue: $newValue")
            ctx.toggleWorkManager(workInterval, newValue)
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

        // Widget Settings
        findPreference<Preference>("open_widget_settings")?.setOnPreferenceClickListener {
            Log.d("open_widget_settings", "setOnPreferenceClickListener")
            findNavController().navigate(R.id.nav_action_settings_widget)
            false
        }
    }

    fun Context.toggleWorkManager(pref: ListPreference, newValue: Any): Boolean {
        Log.d("toggleWorkManager", "newValue: $newValue")
        val value = newValue as? String
        Log.d("toggleWorkManager", "String value: $value")
        if (value.isNullOrEmpty()) {
            Log.w("toggleWorkManager", "NULL OR EMPTY - false")
            return false
        } else if (pref.value != value) {
            Log.i("toggleWorkManager", "RESCHEDULING WORK - true")
            val interval = value.toLongOrNull()
            Log.i("toggleWorkManager", "interval: $interval")
            if (interval != null) {
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
                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "daily_worker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    newRequest
                )
                return true
            } else {
                Log.i("toggleWorkManager", "DISABLING WORK - true")
                WorkManager.getInstance(this).cancelUniqueWork("daily_worker")
                return true
            }
        } else {
            Log.i("toggleWorkManager", "NO CHANGE - false")
            return false
        }
    }

    fun Context.showFeedbackDialog() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_feedback, null)
        val input = view.findViewById<EditText>(R.id.feedback_input)

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

            val link = view.findViewById<TextView>(R.id.github_link)
            val linkText = getString(R.string.github_link, link.tag)
            link.text = Html.fromHtml(linkText, Html.FROM_HTML_MODE_LEGACY)
            link.movementMethod = LinkMovementMethod.getInstance()

            //val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
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

        val sourceText = getString(R.string.github_link, sourceLink.tag)
        Log.d(LOG_TAG, "sourceText: $sourceText")

        val websiteText = getString(R.string.website_link, websiteLink.tag)
        Log.d(LOG_TAG, "websiteText: $websiteText")

        val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(LOG_TAG, "versionName: $versionName")

        val formattedVersion = getString(R.string.version_string, versionName)
        Log.d(LOG_TAG, "formattedVersion: $formattedVersion")

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Close", null)
            .create()

        dialog.setOnShowListener {
            appId.text = this.packageName
            appVersion.text = formattedVersion
            sourceLink.text = Html.fromHtml(sourceText, Html.FROM_HTML_MODE_LEGACY)
            sourceLink.movementMethod = LinkMovementMethod.getInstance()
            websiteLink.text = Html.fromHtml(websiteText, Html.FROM_HTML_MODE_LEGACY)
            websiteLink.movementMethod = LinkMovementMethod.getInstance()
        }
        dialog.show()
    }
}

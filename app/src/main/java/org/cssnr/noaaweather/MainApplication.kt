package org.cssnr.noaaweather

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import org.acra.config.httpSender
import org.acra.config.toast
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender

class MainApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        initAcra {
            // core configuration
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            httpSender {
                uri = BuildConfig.ACRA_URI
                basicAuthLogin = BuildConfig.ACRA_USER
                basicAuthPassword = BuildConfig.ACRA_PASS
                httpMethod = HttpSender.Method.POST
            }
            // toast configuration
            toast {
                text = base.getString(R.string.acra_toast_text)
                length = Toast.LENGTH_LONG
            }
        }
        // Filter out Android framework bug: SecurityException from
        // PhoneFallbackEventHandler sending CLOSE_SYSTEM_DIALOGS broadcast.
        // This crashes on Android 12 emulators and older devices with physical
        // CALL/CAMERA buttons. The fix was applied in AOSP QPR1 but not all
        // builds received it.
        // https://android.googlesource.com/platform/frameworks/base/+/1538b7fda7608f40ee85020785504539a0f02e78%5E%21/
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable.hasCloseSystemDialogsCause()) {
                Log.w("MainApplication", "Ignoring framework CLOSE_SYSTEM_DIALOGS bug", throwable)
                return@setDefaultUncaughtExceptionHandler
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun Throwable.hasCloseSystemDialogsCause(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is SecurityException &&
                current.message?.contains("CLOSE_SYSTEM_DIALOGS") == true
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}

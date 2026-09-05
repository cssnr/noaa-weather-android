package org.cssnr.noaaweather.ui

import android.view.View
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference

object SnackbarManager {
    private var anchorView: WeakReference<View>? = null

    fun init(anchor: View) {
        anchorView = WeakReference(anchor)
    }

    fun show(message: String, long: Boolean = false) {
        val anchor = anchorView?.get() ?: return
        val duration = if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
        Snackbar.make(anchor, message, duration)
            .setAnchorView(anchor)
            .setAction("Close") { }
            .show()
    }
}

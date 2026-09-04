package org.cssnr.noaaweather.ui

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View?.showSnackbar(
    message: String,
    long: Boolean = false,
    anchorView: View? = null,
) {
    val view = this ?: return
    val duration = if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
    val snackbar = Snackbar.make(view, message, duration)
    anchorView?.let { snackbar.setAnchorView(it) }
    snackbar.setAction("Close") { }
    snackbar.show()
}

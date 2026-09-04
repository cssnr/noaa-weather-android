package org.cssnr.noaaweather.ui

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View?.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_LONG,
    anchorView: View? = null,
) {
    val view = this ?: return
    val snackbar = Snackbar.make(view, message, duration)
    anchorView?.let { snackbar.setAnchorView(it) }
    snackbar.setAction("Close") { }
    snackbar.show()
}

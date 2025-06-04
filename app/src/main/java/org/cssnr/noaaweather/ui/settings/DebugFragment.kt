package org.cssnr.noaaweather.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.databinding.FragmentDebugBinding
import java.io.File

class DebugFragment : Fragment() {

    private var _binding: FragmentDebugBinding? = null
    private val binding get() = _binding!!

    private lateinit var logFile: File
    private lateinit var logText: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDebugBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "savedInstanceState: ${savedInstanceState?.size()}")

        val ctx = requireContext()

        lifecycleScope.launch { binding.textView.text = ctx.readLogFile() }

        binding.copyLogs.setOnClickListener {
            Log.d(LOG_TAG, "copyLogs")
            ctx.copyToClipboard(logText, "Logs Copied")
        }

        binding.reloadLogs.setOnClickListener {
            Log.d(LOG_TAG, "reloadLogs")
            lifecycleScope.launch {
                binding.textView.text = ctx.readLogFile()
                Toast.makeText(ctx, "Logs Reloaded.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.clearLogs.setOnClickListener {
            Log.d(LOG_TAG, "clearLogs")
            MaterialAlertDialogBuilder(ctx, R.style.AlertDialogTheme)
                .setIcon(R.drawable.md_delete_24px)
                .setTitle("Confirm")
                .setMessage("Delete All Logs?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ ->
                    logFile.writeText("")
                    logText = ""
                    binding.textView.text = ""
                    Toast.makeText(ctx, "Logs Cleared.", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Setup refresh listener which triggers new data loading
        binding.swiperefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh() {
                Log.d(LOG_TAG, "setOnRefreshListener: onRefresh")
                lifecycleScope.launch {
                    binding.textView.text = ctx.readLogFile()
                    Toast.makeText(ctx, "Logs Reloaded.", Toast.LENGTH_SHORT).show()
                    binding.swiperefresh.isRefreshing = false
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "DebugFragment - onResume")
        lifecycleScope.launch { binding.textView.text = requireContext().readLogFile() }
    }

//    suspend fun Context.readLogFile(): String = withContext(Dispatchers.IO) {
//        File(filesDir, "debug_log.txt").readLines().asReversed().joinToString("\n")
//    }

    suspend fun Context.readLogFile(): String = withContext(Dispatchers.IO) {
        try {
            val file = File(filesDir, "debug_log.txt")
            if (!file.canRead()) {
                Log.e("readLogFile", "Log File Not Found or Not Readable: ${file.absolutePath}")
                return@withContext "Unable to read logs: ${file.absolutePath}"
            }
            file.readLines().asReversed().joinToString("\n")
        } catch (e: Exception) {
            Log.e("readLogFile", "Exception", e)
            "Exception reading logs: ${e.message}"
        }
    }

    fun Context.copyToClipboard(text: String, msg: String? = null) {
        val clipboard = this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, msg ?: "Copied to Clipboard", Toast.LENGTH_SHORT).show()
    }

    //fun Context.parseLog(name: String, default: String = "No Log Entries."): String {
    //    val logFile = File(filesDir, "${name}.txt")
    //    Log.d(LOG_TAG, "logFile: $logFile")
    //    if (!logFile.exists()) return default
    //
    //    val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    //    val builder = StringBuilder()
    //
    //    for (line in logFile.readLines().asReversed()) {
    //        val splitIndex = line.indexOf(" ")
    //        if (splitIndex == -1) continue
    //        val timestampString = line.substring(0, splitIndex)
    //        val message = line.substring(splitIndex + 1)
    //
    //        val dateTime = runCatching {
    //            ZonedDateTime.parse(
    //                timestampString,
    //                DateTimeFormatter.ISO_ZONED_DATE_TIME
    //            )
    //        }.getOrNull()
    //        val instant = dateTime?.toInstant()
    //        val date = Date.from(instant)
    //        val formatted = dateFormat.format(date)
    //
    //        builder.append("$formatted $message\n")
    //    }
    //    if (builder.isEmpty()) return default
    //    if (builder.endsWith("\n")) {
    //        builder.setLength(builder.length - 1)
    //    }
    //    return builder.toString()
    //}
}

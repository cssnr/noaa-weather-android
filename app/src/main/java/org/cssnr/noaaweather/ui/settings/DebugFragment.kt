package org.cssnr.noaaweather.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cssnr.noaaweather.MainActivity.Companion.LOG_FILE
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.copyToClipboard
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

        logFile = File(ctx.filesDir, "${LOG_FILE}.txt")

        logText = logFile.readLines().asReversed().joinToString("\n")
        binding.textView.text = logText

        binding.copyLogs.setOnClickListener {
            Log.d(LOG_TAG, "copyLogs")
            ctx.copyToClipboard(logText, "Logs Copied")
        }

        binding.reloadLogs.setOnClickListener {
            Log.d(LOG_TAG, "reloadLogs")
            logText = logFile.readLines().asReversed().joinToString("\n")
            binding.textView.text = logText
            Toast.makeText(ctx, "Logs Reloaded.", Toast.LENGTH_SHORT).show()
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
                logText = logFile.readLines().asReversed().joinToString("\n")
                binding.textView.text = logText
                Toast.makeText(ctx, "Logs Reloaded.", Toast.LENGTH_SHORT).show()
                binding.swiperefresh.isRefreshing = false
            }
        })
    }

    //override fun onStart() {
    //    super.onStart()
    //    Log.d(LOG_TAG, "DebugFragment - onStart")
    //}
    //override fun onStop() {
    //    super.onStop()
    //    Log.d(LOG_TAG, "DebugFragment - onStop")
    //}
    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "DebugFragment - onResume")
        logText = logFile.readLines().asReversed().joinToString("\n")
        binding.textView.text = logText
    }
    //override fun onPause() {
    //    super.onPause()
    //    Log.d(LOG_TAG, "DebugFragment - onPause")
    //}

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

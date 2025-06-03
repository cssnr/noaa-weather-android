package org.cssnr.noaaweather.ui.settings

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.cssnr.noaaweather.MainActivity.Companion.LOG_FILE
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.copyToClipboard
import org.cssnr.noaaweather.databinding.FragmentDebugBinding
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class DebugFragment : Fragment() {

    private var _binding: FragmentDebugBinding? = null
    private val binding get() = _binding!!

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

        val parsed = ctx.parseLog(LOG_FILE)
        binding.textView.text = parsed

        binding.copyLogs.setOnClickListener {
            Log.d(LOG_TAG, "copyLogs")
            ctx.copyToClipboard(parsed, "Logs Copied")
        }

        binding.reloadLogs.setOnClickListener {
            Log.d(LOG_TAG, "reloadLogs")
            val parsed = ctx.parseLog(LOG_FILE)
            binding.textView.text = parsed
            Toast.makeText(ctx, "Logs Reloaded.", Toast.LENGTH_SHORT).show()
        }

        binding.clearLogs.setOnClickListener {
            Log.d(LOG_TAG, "clearLogs")
            val logFile = File(ctx.filesDir, "${LOG_FILE}.txt")
            logFile.writeText("")
            binding.textView.text = "Log File Cleared."
            Toast.makeText(ctx, "Logs Cleared.", Toast.LENGTH_SHORT).show()
        }
    }

    fun Context.parseLog(name: String, default: String = "No Log Entries."): String {
        val logFile = File(filesDir, "${name}.txt")
        Log.d(LOG_TAG, "logFile: $logFile")
        if (!logFile.exists()) return default

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
        val builder = StringBuilder()

        for (line in logFile.readLines().asReversed()) {
            val splitIndex = line.indexOf(" - ")
            if (splitIndex == -1) continue
            val timestampString = line.substring(0, splitIndex)
            val message = line.substring(splitIndex + 3)

            //val date = runCatching { formatter.parse(timestampString) }.getOrNull()
            //val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
            //val formatted = dateFormat.format(date)

            val dateTime = runCatching {
                ZonedDateTime.parse(
                    timestampString,
                    DateTimeFormatter.ISO_ZONED_DATE_TIME
                )
            }.getOrNull()
            Log.d("WidgetUpdater", "dateTime: $dateTime")
            val instant = dateTime?.toInstant()
            val date = Date.from(instant)
            val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
            val formatted = dateFormat.format(date)

            builder.append("$formatted - $message\n")
        }
        if (builder.isEmpty()) return default
        if (builder.endsWith("\n")) {
            builder.setLength(builder.length - 1)
        }
        return builder.toString()
    }
}

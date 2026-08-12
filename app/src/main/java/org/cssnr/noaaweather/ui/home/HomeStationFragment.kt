package org.cssnr.noaaweather.ui.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.databinding.FragmentHomeBinding
import org.cssnr.noaaweather.db.WeatherStation
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class HomeStationFragment : Fragment(), UpdatableFragment {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val station = if (Build.VERSION.SDK_INT >= 33) {
            arguments?.getParcelable("station", WeatherStation::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("station")
        }
        //Log.d(LOG_TAG, "STEP 2 - onViewCreated: station: $station")
        if (station == null) {
            Log.e(LOG_TAG, "STATION IS NULL")
            return
        }
        updateData(station)
    }

    override fun updateData(newData: WeatherStation) {
        val appContext = requireContext()

        //val sharedPreferences =
        //    appContext.getSharedPreferences("org.cssnr.noaaweather", MODE_PRIVATE)
        val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        val tempUnit = preferences.getString("temp_unit", null) ?: "C"
        //Log.d(LOG_TAG, "tempUnit: $tempUnit")

        // Top
        binding.stationName.text = newData.name

        // Top Middle
        binding.stationId.text = newData.stationId
        binding.stationElevation.text = newData.elevation
        binding.stationCoordinates.text = newData.coordinates
        binding.stationTimestamp.text = formatDate(newData.timestamp)

        // Left Middle
        binding.stationTemperature.text = appContext.getTemp(newData.temperature, tempUnit)
        binding.stationDewpoint.text = appContext.getTemp(newData.dewpoint, tempUnit)
        binding.stationHumidity.text =
            appContext.getValue(R.string.format_percent, newData.relativeHumidity)
        binding.stationWindSpeed.text =
            appContext.getValue(R.string.format_km_h, newData.windSpeed)
        binding.stationWindDirection.text =
            appContext.getValue(R.string.format_direction, newData.windDirection)
        binding.stationPressureBaro.text =
            appContext.getValue(R.string.format_pa, newData.barometricPressure)
        binding.stationPressureSea.text =
            appContext.getValue(R.string.format_pa, newData.seaLevelPressure)
        binding.stationVisibility.text =
            appContext.getValue(R.string.format_meters, newData.visibility)

        // Right Middle
        if (newData.icon != null) {
            Glide.with(appContext).load(newData.icon).into(binding.stationIcon)
        } else {
            binding.stationIcon.setImageDrawable(null)
        }
        binding.linkForecast.setOnClickListener {
            openLink(newData, it.tag as? String)
        }
        binding.linkHourly.setOnClickListener {
            openLink(newData, it.tag as? String)
        }

        //// Bottom - Extras
        //if (!newData.rawMessage.isNullOrEmpty()) {
        //    binding.extrasDivider.visibility = View.VISIBLE
        //    binding.stationHeading.visibility = View.VISIBLE
        //    binding.stationMessage.text = newData.rawMessage
        //}
    }

    private fun openLink(station: WeatherStation, tag: String?) {
        Log.d(LOG_TAG, "${station.stationId} - tag: $tag")
        Log.d(LOG_TAG, "station.coordinates: ${station.coordinates}")
        if (station.coordinates != null) {
            val latitude = station.coordinates.split(",")[1].trim()
            val longitude = station.coordinates.split(",")[0].trim()
            Log.d(LOG_TAG, "Long: \"$latitude\" - \"$longitude\"")
            val (lat, lon) = shortCoords(station.coordinates)
            Log.d(LOG_TAG, "Short: \"$lat\" - \"$lon\"")
            val url = when (tag) {
                "forecast" -> String.format(
                    Locale.US,
                    "https://forecast.weather.gov/MapClick.php?lat=%s&lon=%s",
                    latitude,
                    longitude
                )

                "hourly" -> String.format(
                    Locale.US,
                    "https://forecast.weather.gov/MapClick.php?lat=%s&lon=%s&FcstType=graphical",
                    lat,
                    lon
                )

                else -> null
            }
            Log.d(LOG_TAG, "url: \"$url\"")
            if (url != null) {
                val formattedUrl = String.format(Locale.US, url, lat, lon)
                Log.d(LOG_TAG, "formattedUrl: \"$formattedUrl\"")
                val intent = Intent(Intent.ACTION_VIEW, formattedUrl.toUri())
                startActivity(intent)
            }
        }
    }
}

interface UpdatableFragment {
    fun updateData(newData: WeatherStation)
}

fun formatDate(dateString: String?): String {
    Log.d(LOG_TAG, "formatDate: \"$dateString\"")
    if (dateString.isNullOrEmpty()) return ""
    val zonedDateTime = ZonedDateTime.parse(dateString)
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).format(formatter)
}

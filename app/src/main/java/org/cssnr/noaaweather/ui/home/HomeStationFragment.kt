package org.cssnr.noaaweather.ui.home

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.databinding.FragmentHomeBinding
import org.cssnr.noaaweather.db.WeatherStation
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

    override fun updateData(station: WeatherStation) {
        val appContext = requireContext()

        val sharedPreferences =
            appContext.getSharedPreferences("org.cssnr.noaaweather", MODE_PRIVATE)
        val tempUnit = sharedPreferences.getString("temp_unit", null) ?: "C"
        //Log.d(LOG_TAG, "tempUnit: $tempUnit")

        binding.stationName.text = station.name
        binding.stationMessage.text = station.rawMessage

        binding.stationId.text = station.stationId
        binding.stationElevation.text = station.elevation
        binding.stationCoordinates.text = station.coordinates
        binding.stationTimestamp.text = station.timestamp

        binding.stationTemperature.text = appContext.getTemp(station.temperature, tempUnit)
        binding.stationDewpoint.text = appContext.getTemp(station.dewpoint, tempUnit)

        binding.stationHumidity.text =
            appContext.getValue(R.string.format_percent, station.relativeHumidity)
        binding.stationWindSpeed.text =
            appContext.getValue(R.string.format_km_h, station.windSpeed)
        binding.stationWindDirection.text =
            appContext.getValue(R.string.format_direction, station.windDirection)
        binding.stationPressureBaro.text =
            appContext.getValue(R.string.format_pa, station.barometricPressure)
        binding.stationPressureSea.text =
            appContext.getValue(R.string.format_pa, station.seaLevelPressure)
        binding.stationVisibility.text =
            appContext.getValue(R.string.format_meters, station.visibility)

        if (station.icon != null) {
            Log.d(LOG_TAG, "station.icon: ${station.icon}")
            Glide.with(appContext).load(station.icon).into(binding.stationIcon)
        } else {
            binding.stationIcon.setImageDrawable(null)
        }

        binding.linkForecast.setOnClickListener {
            openLink(station, it.tag as? String)
        }
        binding.linkHourly.setOnClickListener {
            openLink(station, it.tag as? String)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

package org.cssnr.noaaweather.ui.home

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.databinding.FragmentHomeBinding
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.db.WeatherStation
import org.cssnr.noaaweather.ui.stations.getCurrentConditions
import java.io.InputStream
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "HomeFragment - onViewCreated: ${savedInstanceState?.size()}")

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "NOAAWeather Android")
                    .build()
                chain.proceed(request)
            }
            .build()
        val okHttpUrlLoader = OkHttpUrlLoader.Factory(okHttpClient)
        Glide.get(requireContext()).registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            okHttpUrlLoader
        )

        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        val appContext = requireContext()

        val sharedPreferences =
            appContext.getSharedPreferences("org.cssnr.noaaweather", MODE_PRIVATE)

        homeViewModel.data.observe(viewLifecycleOwner) { station ->
            Log.i(LOG_TAG, "homeViewModel.data.observe: $station")

            val tempUnit = sharedPreferences.getString("temp_unit", null) ?: "C"
            Log.i(LOG_TAG, "tempUnit: $tempUnit")

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

        lifecycleScope.launch {
            val dao = StationDatabase.getInstance(appContext).stationDao()
            val station = withContext(Dispatchers.IO) {
                dao.getActive()
            }
            Log.d(LOG_TAG, "station: $station")
            if (station != null) {
                Log.i(LOG_TAG, "INITIALIZE VIEW MODEL")
                homeViewModel.data.value = station
            } else {
                Log.w(LOG_TAG, "STATION IS NULL")
                //Toast.makeText(this, "Station Not Found!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.refreshDashboard.setOnClickListener { view ->
            Log.d(LOG_TAG, "binding.refreshDashboard.setOnClickListener")

            lifecycleScope.launch {
                val dao = StationDatabase.getInstance(appContext).stationDao()
                val station = withContext(Dispatchers.IO) {
                    dao.getActive()
                }
                Log.d(LOG_TAG, "station: $station")
                if (station != null) {
                    val current = withContext(Dispatchers.IO) {
                        appContext.getCurrentConditions(station.stationId)
                    }
                    Log.i(LOG_TAG, "UPDATE VIEW MODEL")
                    Log.d(LOG_TAG, "current: $current")
                    homeViewModel.data.value = current
                }
            }

            Snackbar.make(view, "Dashboard Updated", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.refresh_dashboard).show()
        }

        //// API Code
        //val api = DiscordApi(
        //    requireContext(),
        //    "https://discord.com/api/webhooks/000/xxx"
        //)
        //lifecycleScope.launch {
        //    val response = api.sendMessage("Home Fragment")
        //    Log.d(LOG_TAG, "response: $response")
        //}

        // Geocoder Code
        //fun callback(lat: Double?, lon: Double?) {
        //    Log.d(LOG_TAG, "$lat / $lon")
        //}
        //val geocoder = Geocoder(requireContext())
        //lifecycleScope.launch {
        //    geocoder.getLocation("Seattle, WA", ::callback)
        //}

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openLink(station: WeatherStation, tag: String?) {
        Log.d(LOG_TAG, "${station.stationId} - tag: $tag")
        Log.d(LOG_TAG, "station.coordinates: ${station.coordinates}")
        if (station.coordinates != null) {
            // TODO: Add function to format latitude/longitude...
            val latitude = station.coordinates.split(",")[1].trim()
            val longitude = station.coordinates.split(",")[0].trim()
            Log.d(LOG_TAG, "\"$longitude\" - \"$longitude\"")
            val lat = String.format(Locale.US, "%.4f", latitude.toDouble())
            val lon = String.format(Locale.US, "%.4f", longitude.toDouble())
            Log.d(LOG_TAG, "\"$lat\" - \"$lon\"")
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

fun Context.getValue(stringId: Int, value: Double?): String {
    if (value == null) {
        return "N/A"
    }
    return getString(stringId, value)
}

fun Context.getTemp(value: Double?, unit: String? = "C"): String {
    //val tempF = (value * 9/5) + 32
    if (value == null) {
        return "N/A"
    }
    val temp = if (unit == "F") (value * 9 / 5) + 32 else value
    Log.d(LOG_TAG, "unit: $unit - value: $value - temp: $temp")
    val formatted = this.getString(R.string.format_temp, temp, unit)
    Log.d(LOG_TAG, "formatted: $formatted")
    return formatted
}


//@Suppress("DEPRECATION")
//fun Geocoder.getAddress(
//    latitude: Double,
//    longitude: Double,
//    address: (android.location.Address?) -> Unit
//) {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        Log.d("Dash[onViewCreated]", "Build.VERSION_CODES.TIRAMISU")
//        getFromLocation(latitude, longitude, 1) { address(it.firstOrNull()) }
//        return
//    }
//    try {
//        Log.d("Dash[onViewCreated]", "TRY")
//        address(getFromLocation(latitude, longitude, 1)?.firstOrNull())
//    } catch(e: Exception) {
//        Log.e("Dash[onViewCreated]", "Exception: $e")
//        address(null)
//    }
//}

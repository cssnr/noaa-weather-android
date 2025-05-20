package org.cssnr.noaaweather.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import org.cssnr.noaaweather.ui.stations.getCurrentConditions
import java.io.InputStream

//import androidx.lifecycle.lifecycleScope
//import kotlinx.coroutines.launch
//import org.cssnr.noaaweather.api.DiscordApi

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

        homeViewModel.data.observe(viewLifecycleOwner) { station ->
            Log.i(LOG_TAG, "homeViewModel.data.observe: $station")
            binding.stationName.text = station.name
            binding.stationMessage.text = station.rawMessage

            binding.stationId.text = station.stationId
            binding.stationElevation.text = station.elevation
            binding.stationCoordinates.text = station.coordinates

            binding.stationTimestamp.text = station.timestamp

            binding.stationTemperature.text = station.temperature ?: "-"
            binding.stationDewpoint.text = station.dewpoint ?: "-"
            binding.stationHumidity.text = station.relativeHumidity ?: "-"
            binding.stationWindSpeed.text = station.windSpeed ?: "-"
            binding.stationWindDirection.text = station.windDirection ?: "-"
            binding.stationPressureBaro.text = station.barometricPressure ?: "-"
            binding.stationPressureSea.text = station.seaLevelPressure ?: "-"
            binding.stationVisibility.text = station.visibility ?: "-"

            if (station.icon != null) {
                Log.d(LOG_TAG, "station.icon: ${station.icon}")
                Glide.with(appContext).load(station.icon).into(binding.stationIcon)
            } else {
                binding.stationIcon.setImageDrawable(null)
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
            //val api = WeatherApi(appContext)
            //val response = api.getLatest(data.stationId)
            //Log.d(LOG_TAG, "response.isSuccessful: ${response.isSuccessful}")
            //val latest = response.body()
            //Log.d(LOG_TAG, "latest: $latest")
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
        //    "https://discord.com/api/webhooks/882795463856750694/B9Cc_JOpkfdnPm3I4m8Z0KKSfGVyZtHIDDmf4TMdisKgJ4uX_UWa3qooHVY2yBgTMM2X"
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

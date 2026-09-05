package org.cssnr.noaaweather.ui.stations.add

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.api.WeatherApi
import org.cssnr.noaaweather.api.WeatherApi.ObservationStationsResponse
import org.cssnr.noaaweather.databinding.FragmentAddStationBinding
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.db.WeatherStation
import org.cssnr.noaaweather.log.DebugLogger
import org.cssnr.noaaweather.ui.SnackbarManager
import org.cssnr.noaaweather.ui.stations.updateStation
import java.io.IOException
import java.util.Locale

const val LOG_TAG = "AddStation"

class AddStationFragment : Fragment() {

    private var _binding: FragmentAddStationBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: AddStationAdapter

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(LOG_TAG, "locationPermissionLauncher: isGranted: $isGranted")
        if (!isAdded) return@registerForActivityResult
        if (isGranted) {
            requestLocation()
        } else {
            SnackbarManager.show("Location Not Allowed", true)

            //val permanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(
            //    requireActivity(),
            //    Manifest.permission.ACCESS_FINE_LOCATION
            //)
            //Log.d(LOG_TAG, "permanentlyDenied: $permanentlyDenied")
            //if (permanentlyDenied) {
            //    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            //        data = Uri.fromParts("package", requireContext().packageName, null)
            //    }
            //    requireContext().startActivity(intent)
            //}
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddStationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "onViewCreated")

        val appContext = requireContext().applicationContext

        // Adapter
        binding.suggestionsList.layoutManager = LinearLayoutManager(appContext)
        adapter = AddStationAdapter(emptyList()) { data ->
            Log.d(LOG_TAG, "onItemClick: $data")
            addStation(data)
        }
        binding.suggestionsList.adapter = adapter

        // Input Text
        binding.location.requestFocus()
        binding.location.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                Log.d(LOG_TAG, "afterTextChanged: $text")
                val query = text?.toString().orEmpty()
                if (query.isEmpty()) {
                    return
                }
                getPlaceLocation(query) { result ->
                    if (!isAdded) return@getPlaceLocation
                    when (result) {
                        is AddressResult.Success -> {
                            val addresses = result.addresses
                            Log.d(LOG_TAG, "addresses: $addresses")
                            context?.let { ctx ->
                                lifecycleScope.launch {
                                    val message =
                                        "Found ${addresses.size} Addresses: ${addresses.firstOrNull()?.featureName}"
                                    DebugLogger.d(ctx, message)
                                }
                            }
                            if (addresses.isNotEmpty()) {
                                lifecycleScope.launch {
                                    if (!isAdded) return@launch
                                    val ctx = context ?: return@launch
                                    val data = ctx.getStations(
                                        addresses[0].latitude,
                                        addresses[0].longitude
                                    )
                                    Log.d(LOG_TAG, "data: $data")
                                    when (data) {
                                        is StationsResult.Success -> {
                                            if (!isAdded || _binding == null) return@launch
                                            adapter.updateData(data.response)
                                        }
                                        is StationsResult.Error -> {
                                            if (!isAdded || _binding == null) return@launch
                                            showMessage(data.message)
                                        }
                                    }
                                }
                            } else {
                                showMessage("No results found for \"$query\"")
                            }
                        }
                        is AddressResult.Error -> {
                            Log.e(LOG_TAG, "getPlaceLocation error: ${result.message}")
                            showMessage(result.message)
                        }
                    }
                }
            }

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
            }
        })

        // Cancel Button
        binding.btnCancel.setOnClickListener {
            Log.d(LOG_TAG, "CANCEL")
            findNavController().navigateUp()
        }

        // Locate Button
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(appContext)
        binding.btnLocate.setOnClickListener {
            if (isAdded) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    override fun onDestroyView() {
        searchRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        searchRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    private fun getPlaceLocation(place: String, callback: (AddressResult) -> Unit) {
        Log.d(LOG_TAG, "getPlaceLocation: place: $place")
        searchRunnable?.let { handler.removeCallbacks(it) }
        searchRunnable = Runnable {
            if (!isAdded) return@Runnable
            if (place.isNotEmpty()) {
                val ctx = context ?: return@Runnable
                lifecycleScope.launch { DebugLogger.d(ctx, "Searching Place: $place") }
                val geocoder = Geocoder(ctx)
                lifecycleScope.launch(Dispatchers.IO) {
                    geocoder.getLocation(place) { result ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (!isAdded) return@launch
                            Log.d(LOG_TAG, "getLocation result: $result")
                            callback(result)
                        }
                    }
                }
            }
        }
        handler.postDelayed(searchRunnable!!, 1000)
    }

    //@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocation() {
        Log.d("requestLocation", "START")
        if (!isAdded) return
        val ctx = context ?: return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (!isAdded) return
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100
            )
            Log.d("requestLocation", "RETURN")
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (!isAdded) return@addOnSuccessListener
            if (location != null) {
                Log.d("Location", "location: $location")
                Log.d("Location", "lat/lon: ${location.latitude} / ${location.longitude}")
                lifecycleScope.launch(Dispatchers.IO) {
                    if (!isAdded) return@launch
                    val ioCtx = context ?: return@launch
                    val data = ioCtx.getStations(location.latitude, location.longitude)
                    Log.d("Location", "data: $data")
                    withContext(Dispatchers.Main) {
                        if (!isAdded || _binding == null) return@withContext
                        //val stringData = data.response.features.map { it.properties.name }
                        //Log.d("Location", "stringData: $stringData")
                        when (data) {
                            is StationsResult.Success -> adapter.updateData(data.response)
                            is StationsResult.Error -> showMessage(data.message)
                        }
                    }
                }
            } else {
                Log.w("Location", "Location is null")
                showMessage("Unable to get your location")
            }
        }.addOnFailureListener {
            Log.e("Location", "Failed to get location: ${it.message}")
            showMessage("Failed to get location: ${it.message}")
        }
    }

    private fun addStation(data: ObservationStationsResponse.Feature) {
        val appContext = requireContext().applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = StationDatabase.getInstance(appContext).stationDao()
            //val existing = dao.getById(data.properties.stationIdentifier)
            //Log.d(LOG_TAG, "existing: $existing")
            val elevationValue =
                String.format(Locale.US, "%.1f", data.properties.elevation.value)
            val elevation =
                "$elevationValue ${data.properties.elevation.unitCode.split(":")[1]}"
            Log.d(LOG_TAG, "elevation: $elevation")
            dao.deactivateAllStations()
            val station = WeatherStation(
                stationId = data.properties.stationIdentifier,
                name = data.properties.name,
                elevation = elevation,
                coordinates = getCoordinates(data.geometry.coordinates),
                forecast = data.properties.forecast,
                active = true,
            )
            Log.d(LOG_TAG, "station: $station")
            dao.add(station)
            // Fire-and-forget station update using application context (safe after detach)
            CoroutineScope(Dispatchers.IO).launch { appContext.updateStation(station.stationId) }
            Log.i(LOG_TAG, "savedStateHandle: stations_updated: ${station.stationId}")
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                findNavController().previousBackStackEntry?.savedStateHandle
                    ?.set("stations_updated", station.stationId)
                findNavController().navigateUp()
            }
        }
    }

    private fun getCoordinates(coordinates: List<Double>): String {
        return "${coordinates[0]}, ${coordinates[1]}"
    }

    private fun showMessage(message: String) {
        Log.i(LOG_TAG, "showMessage: $message")
        //adapter.updateData(ObservationStationsResponse(emptyList()))
        SnackbarManager.show(message, true)
    }
}

sealed interface StationsResult {
    data class Success(val response: ObservationStationsResponse) : StationsResult
    data class Error(val message: String) : StationsResult
}

suspend fun Context.getStations(
    latitude: Double,
    longitude: Double
): StationsResult {
    Log.d("getStations", "getStations: $latitude / $longitude")
    val api = WeatherApi(this)
    val response = api.getStationFromPoint(latitude, longitude)
    Log.d("getStations", "response: $response")
    if (response == null) {
        return StationsResult.Error("Failed to load stations")
    }
    val stationsResponse = response.body()
    Log.d("getStations", "stationsResponse?.features?.size: ${stationsResponse?.features?.size}")
    if (stationsResponse == null) {
        return StationsResult.Error("Failed to load stations: HTTP ${response.code()}")
    }
    if (stationsResponse.features.isEmpty()) {
        return StationsResult.Error("No weather stations found near this location")
    }
    val message = "Found ${stationsResponse.features.size} stations for: $latitude / $longitude"
    DebugLogger.i(this, message)
    return StationsResult.Success(stationsResponse)
}

sealed interface AddressResult {
    data class Success(val addresses: MutableList<Address>) : AddressResult
    data class Error(val message: String) : AddressResult
}

@Suppress("DEPRECATION")
suspend fun Geocoder.getLocation(
    name: String,
    maxResults: Int = 5,
    callback: (AddressResult) -> Unit,
) {
    Log.d("getLocation", "getLocation: maxResults: $maxResults - $name")
    if (!Geocoder.isPresent()) {
        Log.w("getLocation", "Geocoder not present")
        callback(AddressResult.Error("Geocoder not available on this device"))
        return
    }
    val result = try {
        withContext(Dispatchers.IO) {
            this@getLocation.getFromLocationName(name, maxResults)
        }
    } catch (e: IOException) {
        Log.e("getLocation", "Geocoder IOException: ${e.message}")
        callback(AddressResult.Error("Geocoder error: ${e.message}"))
        return
    }
    if (result.isNullOrEmpty()) {
        callback(AddressResult.Error("No results found for \"$name\""))
    } else {
        callback(AddressResult.Success(result))
    }
}
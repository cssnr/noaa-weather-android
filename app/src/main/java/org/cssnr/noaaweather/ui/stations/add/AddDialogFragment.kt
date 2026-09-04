package org.cssnr.noaaweather.ui.stations.add

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.api.WeatherApi
import org.cssnr.noaaweather.api.WeatherApi.ObservationStationsResponse
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.db.WeatherStation
import org.cssnr.noaaweather.log.DebugLogger
import org.cssnr.noaaweather.ui.stations.updateStation
import java.io.IOException
import java.util.Locale

const val LOG_TAG = "AddDialog"

class AddDialogFragment : DialogFragment() {

    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var emptyListView: LinearLayout? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: AddDialogAdapter

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(LOG_TAG, "locationPermissionLauncher: isGranted: $isGranted")
        if (!isAdded) return@registerForActivityResult
        if (isGranted) {
            requestLocation()
        } else {
            val ctx = context ?: return@registerForActivityResult
            val msg = "Location Not Allowed"
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            //dialog?.window?.decorView.showSnackbar(msg, true)

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

    @SuppressLint("InflateParams", "MissingPermission")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater

        builder.setView(inflater.inflate(R.layout.dialog_add, null))
        val dialog = builder.create()

        val appContext = requireContext().applicationContext

        dialog.setOnShowListener {
            emptyListView = dialog.findViewById(R.id.empty_layout)

            // Adapter
            val suggestionsList = dialog.findViewById<RecyclerView>(R.id.suggestions_list)
            suggestionsList?.layoutManager = LinearLayoutManager(appContext)
            adapter = AddDialogAdapter(emptyList()) { data ->
                Log.d(LOG_TAG, "onItemClick: $data")
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
                    Log.i(LOG_TAG, "setFragmentResult: stations_updated: ${station.stationId}")
                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        setFragmentResult(
                            "stations_updated",
                            Bundle().apply { putString("stationId", station.stationId) }
                        )
                        // dialog may already be dismissed if fragment detached
                        if (dialog.isShowing) dialog.dismiss()
                    }
                }
            }
            suggestionsList?.adapter = adapter

            // Input Text
            val inputField = dialog.findViewById<EditText>(R.id.location)
            inputField?.requestFocus()
            inputField?.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(text: Editable?) {
                    Log.d(LOG_TAG, "afterTextChanged: $text")
                    getPlaceLocation(text.toString()) { addresses ->
                        if (!isAdded) return@getPlaceLocation
                        Log.d(LOG_TAG, "addresses: $addresses")
                        context?.let { ctx ->
                            lifecycleScope.launch {
                                DebugLogger.d(ctx, "Found ${addresses?.size} Addresses: ${addresses?.firstOrNull()?.featureName}")
                            }
                        }
                        if (!addresses.isNullOrEmpty()) {
                            lifecycleScope.launch {
                                if (!isAdded) return@launch
                                val ctx = context ?: return@launch
                                val data = ctx.getStations(
                                    addresses[0].latitude,
                                    addresses[0].longitude
                                )
                                Log.d(LOG_TAG, "data.features.size: ${data?.features?.size}")
                                if (data != null) {
                                    withContext(Dispatchers.Main) {
                                        emptyListView?.visibility = View.GONE
                                        adapter.updateData(data)
                                    }
                                } else {
                                    Log.i(LOG_TAG, "NO STATION RESULTS!") // TODO: Handle Error
                                }
                            }
                        } else {
                            Log.i(LOG_TAG, "NO LOCATION RESULTS!") // TODO: Handle Error
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
            dialog.findViewById<Button>(R.id.btn_cancel)?.setOnClickListener {
                Log.d(LOG_TAG, "CANCEL")
                dialog.cancel()
            }

            //// Search Button
            //dialog.findViewById<Button>(R.id.btn_search)?.setOnClickListener {
            //    Log.d(LOG_TAG, "SEARCH")
            //    //dialog.dismiss()
            //}

            // Locate Button
            fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(appContext)
            val requestButton = dialog.findViewById<Button>(R.id.btn_locate)
            requestButton?.setOnClickListener {
                if (isAdded && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        return dialog
    }

    private fun getPlaceLocation(place: String, callback: (MutableList<Address>?) -> Unit) {
        Log.d(LOG_TAG, "getPlaceLocation: place: $place")
        searchRunnable?.let { handler.removeCallbacks(it) }
        searchRunnable = Runnable {
            if (!isAdded) return@Runnable
            if (place.isNotEmpty()) {
                val ctx = context ?: return@Runnable
                lifecycleScope.launch { DebugLogger.d(ctx, "Searching Place: $place") }
                val geocoder = Geocoder(ctx)
                lifecycleScope.launch(Dispatchers.IO) {
                    geocoder.getLocation(place) { addresses ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (!isAdded) return@launch
                            Log.d(LOG_TAG, "addresses?.size: ${addresses?.size}")
                            Log.d(LOG_TAG, "addresses: $addresses")
                            callback(addresses)
                        }
                    }
                }
            }
        }
        handler.postDelayed(searchRunnable!!, 1000)
    }

    override fun onDestroyView() {
        searchRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroyView()
    }

    override fun onDestroy() {
        searchRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
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
                    Log.d("Location", "data.features.size: ${data?.features?.size}")
                    if (data != null) {
                        //val stringData = data.features.map { it.properties.name }
                        //Log.d("Location", "stringData: $stringData")
                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            emptyListView?.visibility = View.GONE
                            adapter.updateData(data)
                        }
                    }
                }
            } else {
                Log.w("Location", "Location is null")
            }
        }.addOnFailureListener {
            Log.e("Location", "Failed to get location: ${it.message}")
        }
    }

    private fun getCoordinates(coordinates: List<Double>): String {
        return "${coordinates[0]}, ${coordinates[1]}"
    }
}

suspend fun Context.getStations(
    latitude: Double,
    longitude: Double
): ObservationStationsResponse? {
    Log.d("getStations", "getStations: $latitude / $longitude")
    val api = WeatherApi(this)
    val response = api.getStationFromPoint(latitude, longitude)
    Log.d("getStations", "response: $response")
    val stationsResponse = response?.body()
    Log.d("getStations", "stationsResponse?.features?.size: ${stationsResponse?.features?.size}")
    DebugLogger.i(this, "Found ${stationsResponse?.features?.size} stations for: $latitude / $longitude")
    return stationsResponse
}


@Suppress("DEPRECATION")
suspend fun Geocoder.getLocation(
    name: String,
    maxResults: Int = 5,
    callback: (addresses: MutableList<Address>?) -> Unit,
) {
    Log.d("getLocation", "getLocation: maxResults: $maxResults - $name")
    if (Build.VERSION.SDK_INT >= 33) {
        Log.d("getLocation", "SDK_INT >= 33")
        this.getFromLocationName(name, maxResults, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                callback(addresses)
            }

            override fun onError(errorMessage: String?) {
                Log.e("getLocation", "errorMessage: $errorMessage")
                callback(null)
            }
        })
    } else {
        Log.d("getLocation", "DEPRECATION: SDK_INT < 33")
        if (!Geocoder.isPresent()) {
            Log.w("getLocation", "Geocoder not present")
            callback(null)
            return
        }
        val result = withContext(Dispatchers.IO) {
            try {
                this@getLocation.getFromLocationName(name, maxResults)
            } catch (e: IOException) {
                Log.e("getLocation", "Geocoder IOException: ${e.message}")
                null
            }
        }
        if (!result.isNullOrEmpty()) {
            callback(result)
        } else {
            callback(null)
        }
    }
}

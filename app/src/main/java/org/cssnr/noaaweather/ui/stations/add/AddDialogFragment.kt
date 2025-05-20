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
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.api.WeatherApi
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.db.WeatherStation
import org.cssnr.noaaweather.ui.stations.getCurrentConditions
import java.util.Locale

class AddDialogFragment : DialogFragment() {

    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: AddDialogAdapter

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(LOG_TAG, "locationPermissionLauncher: isGranted: $isGranted")
        if (isGranted) {
            requestLocation()
        } else {
            val msg = "Location Not Allowed"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
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

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("InflateParams", "MissingPermission")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            builder.setView(inflater.inflate(R.layout.dialog_add, null))
            val dialog = builder.create()

            val appContext = requireContext()

            dialog.setOnShowListener {
                dialog.findViewById<Button>(R.id.btn_cancel)?.setOnClickListener {
                    Log.d(LOG_TAG, "CANCEL")
                    dialog.cancel()
                }
                dialog.findViewById<Button>(R.id.btn_search)?.setOnClickListener {
                    Log.d(LOG_TAG, "SEARCH")
                    //dialog.dismiss()
                }

                fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(appContext)

                val requestButton = dialog.findViewById<Button>(R.id.btn_locate)
                requestButton?.setOnClickListener {
                    //requestLocation()
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }

                val suggestionsList = dialog.findViewById<RecyclerView>(R.id.suggestions_list)
                suggestionsList?.layoutManager = LinearLayoutManager(appContext)
                adapter = AddDialogAdapter(emptyList()) { data ->
                    Log.d(LOG_TAG, "onItemClick: $data")
                    CoroutineScope(Dispatchers.IO).launch {
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
                        GlobalScope.launch { appContext.getCurrentConditions(station.stationId) }
                        Log.i(LOG_TAG, "setFragmentResult: stations_updated: ${station.stationId}")
                        withContext(Dispatchers.Main) {
                            setFragmentResult(
                                "stations_updated",
                                bundleOf("stationId" to station.stationId)
                            )
                            dialog.dismiss()
                        }
                    }
                }
                suggestionsList?.adapter = adapter

                //val loadingText = dialog.findViewById<TextView>(R.id.loading_text)
                val inputField = dialog.findViewById<EditText>(R.id.location)
                inputField?.requestFocus()

                //inputField?.addTextChangedListener(object : TextWatcher {
                //    override fun afterTextChanged(text: Editable?) {
                //        Log.d(LOG_TAG, "afterTextChanged: $text")
                //        searchRunnable?.let { handler.removeCallbacks(it) }
                //        val query = text.toString()
                //        searchRunnable = Runnable {
                //            Log.i(LOG_TAG, "QUERY: $query")
                //            if (query.isNotEmpty()) {
                //                val geocoder = Geocoder(requireContext())
                //                lifecycleScope.launch(Dispatchers.IO) {
                //                    geocoder.getLocation(query) { addresses ->
                //                        lifecycleScope.launch(Dispatchers.Main) {
                //                            Log.d(LOG_TAG, "addresses?.size: ${addresses?.size}")
                //                            Log.d(LOG_TAG, "addresses: $addresses")
                //                            if (!addresses.isNullOrEmpty()) {
                //                                loadingText?.visibility = View.GONE
                //                                val adminAreas =
                //                                    addresses.map { it.getAddressLine(0) }
                //                                adapter.updateData(adminAreas)
                //                            }
                //                        }
                //                    }
                //                }
                //            } else {
                //                val results = listOf<String>()
                //                adapter.updateData(results)
                //                loadingText?.visibility = View.VISIBLE
                //            }
                //        }
                //        handler.postDelayed(searchRunnable!!, 1000)
                //    }
                //
                //    override fun beforeTextChanged(
                //        s: CharSequence?, start: Int, count: Int, after: Int
                //    ) {
                //    }
                //
                //    override fun onTextChanged(
                //        s: CharSequence?, start: Int, before: Int, count: Int
                //    ) {
                //    }
                //})
            }

            return dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    //@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocation() {
        Log.d("requestLocation", "START")
        val context = requireContext()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100
            )
            Log.d("requestLocation", "RETURN")
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val loadingText = dialog?.findViewById<TextView>(R.id.loading_text)
                Log.d("Location", "location: $location")
                Log.d("Location", "lat/lon: ${location.latitude} / ${location.longitude}")
                CoroutineScope(Dispatchers.IO).launch {
                    val data = requireContext().getStations(location.latitude, location.longitude)
                    Log.d("Location", "data.features.size: ${data?.features?.size}")
                    if (data != null) {
                        //val stringData = data.features.map { it.properties.name }
                        //Log.d("Location", "stringData: $stringData")
                        withContext(Dispatchers.Main) {
                            adapter.updateData(data)
                            loadingText?.visibility = View.GONE
                        }
                    } else {
                        withContext(Dispatchers.Main) { loadingText?.visibility = View.VISIBLE }
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
): WeatherApi.ObservationStationsResponse? {
    Log.d(LOG_TAG, "getStations: $latitude / $longitude")
    val api = WeatherApi(this)
    val response = api.getStationFromPoint(latitude, longitude)
    Log.d(LOG_TAG, "response: $response")
    val stationsResponse = response?.body()
    Log.d(LOG_TAG, "stationsResponse?.features?.size: ${stationsResponse?.features?.size}")
    return stationsResponse
}


@Suppress("DEPRECATION")
suspend fun Geocoder.getLocation(
    name: String,
    maxResults: Int = 5,
    callback: (addresses: MutableList<Address>?) -> Unit,
) {
    Log.d(LOG_TAG, "getLocation: maxResults: $maxResults - $name")
    if (Build.VERSION.SDK_INT >= 33) {
        Log.d(LOG_TAG, "SDK_INT >= 33")
        this.getFromLocationName(name, maxResults, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                callback(addresses)
            }

            override fun onError(errorMessage: String?) {
                Log.e(LOG_TAG, "errorMessage: $errorMessage")
                callback(null)
            }
        })
    } else {
        Log.d(LOG_TAG, "DEPRECATION: SDK_INT < 33")
        val result = withContext(Dispatchers.IO) {
            this@getLocation.getFromLocationName(name, maxResults)
        }
        if (!result.isNullOrEmpty()) {
            callback(result)
        } else {
            callback(null)
        }
    }
}

package org.cssnr.noaaweather.ui.stations

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.api.WeatherApi
import org.cssnr.noaaweather.databinding.FragmentStationsBinding
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.db.WeatherStation
import org.cssnr.noaaweather.ui.stations.add.AddDialogFragment
import timber.log.Timber

//import androidx.lifecycle.Observer
//import androidx.lifecycle.ViewModelProvider

class StationsFragment : Fragment() {

    private var _binding: FragmentStationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: StationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStationsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "StationsFragment - onViewCreated: ${savedInstanceState?.size()}")

        val appContext = requireContext()
        //val stationsViewModel = ViewModelProvider(this)[StationsViewModel::class.java]

        fun onClick(data: WeatherStation) {
            Log.i(LOG_TAG, "onClick: $data")
            lifecycleScope.launch {
                if (!data.active) {
                    val dao = StationDatabase.getInstance(appContext).stationDao()
                    Log.d(LOG_TAG, "Activating: ${data.stationId}")
                    val stations = withContext(Dispatchers.IO) {
                        dao.deactivateAllStations()
                        dao.activate(data.stationId)
                        dao.getAll()
                    }
                    //Log.d(LOG_TAG, "stations: $stations")
                    adapter.updateData(stations)
                }

                withContext(Dispatchers.IO) { appContext.updateStation(data.stationId) }

                //val api = WeatherApi(appContext)
                //val response = api.getLatest(data.stationId)
                //Log.d(LOG_TAG, "response.isSuccessful: ${response.isSuccessful}")
                //val latest = response.body()
                //Log.d(LOG_TAG, "latest: $latest")
            }
        }

        fun onLongClick(data: WeatherStation) {
            Log.d(LOG_TAG, "onLongClick: ${data.stationId}")
            fun callback(station: WeatherStation) {
                Log.d(LOG_TAG, "callback: ${data.stationId}")
                lifecycleScope.launch {
                    val dao = StationDatabase.getInstance(appContext).stationDao()
                    Log.i(LOG_TAG, "DELETING: ${data.stationId}")
                    val stations = withContext(Dispatchers.IO) {
                        dao.delete(station)
                        if (station.active) {
                            Log.d(LOG_TAG, "activateFirstStation")
                            dao.activateFirstStation()
                        }
                        dao.getAll()
                    }
                    adapter.updateData(stations)
                    //stationsViewModel.stationData.value = stations
                }
            }
            appContext.deleteConfirmDialog(data, ::callback)
        }

        // Initialize Adapter
        if (!::adapter.isInitialized) {
            Log.i(LOG_TAG, "INITIALIZE: StationsAdapter")
            adapter = StationsAdapter(emptyList(), ::onClick, ::onLongClick)
        }
        binding.stationsList.layoutManager = LinearLayoutManager(appContext)
        if (binding.stationsList.adapter == null) {
            Log.i(LOG_TAG, "INITIALIZE: stationsList.adapter")
            binding.stationsList.adapter = adapter
        }

        //// Create the observer which updates the UI.
        //val stationObserver = Observer<List<WeatherStation>> { data ->
        //    Log.d(LOG_TAG, "Observer - data.size: ${data.size}")
        //    //adapter.updateData(data)
        //}
        //// Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        //stationsViewModel.stationData.observe(requireActivity(), stationObserver)

        lifecycleScope.launch {
            val dao = StationDatabase.getInstance(appContext).stationDao()
            val stations = withContext(Dispatchers.IO) { dao.getAll() }
            Log.d(LOG_TAG, "stations.size ${stations.size}")
            adapter.updateData(stations)
            //stationsViewModel.stationData.value = stations
        }

        setFragmentResultListener("stations_updated") { _, bundle ->
            val stationId = bundle.getString("stationId")
            Log.d("setFragmentResultListener", "stationId: $stationId")
            if (stationId != null) {
                Log.i("setFragmentResultListener", "Added stationId: $stationId")
                lifecycleScope.launch {
                    val dao = StationDatabase.getInstance(appContext).stationDao()
                    val stations = withContext(Dispatchers.IO) { dao.getAll() }
                    Log.d(LOG_TAG, "stations.size: ${stations.size}")
                    //stationsViewModel.stationData.value = stations
                    withContext(Dispatchers.Main) { adapter.updateData(stations) }
                }
            }
        }

        binding.addStation.setOnClickListener { view ->
            Log.d(LOG_TAG, "binding.appBarMain.fab.setOnClickListener")
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            //    .setAction("Action", null)
            //    .setAnchorView(R.id.fab).show()
            val newFragment = AddDialogFragment()
            newFragment.show(parentFragmentManager, "AddDialogFragment")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "StationsFragment - onDestroyView")
        _binding = null
    }
}

private fun Context.deleteConfirmDialog(
    station: WeatherStation,
    callback: (station: WeatherStation) -> Unit,
) {
    Log.d("deleteConfirmDialog", "station: ${station.stationId}")
    MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
        .setTitle("Delete ${station.stationId}?")
        .setIcon(R.drawable.md_delete_24px)
        .setMessage(station.name)
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Delete") { _, _ -> callback(station) }
        .show()
}

suspend fun Context.updateStation(stationId: String): WeatherStation? {
    val api = WeatherApi(this)
    val response = api.getLatest(stationId)
    Log.i(LOG_TAG, "response: ${response.code()} - isSuccessful: ${response.isSuccessful}")

    val dao = StationDatabase.getInstance(this).stationDao()
    val current = dao.getById(stationId)
    Log.d(LOG_TAG, "current: $current")

    if (!response.isSuccessful) {
        Timber.w("Update Station Error: ${response.code()} - ${response.message()}")
    } else if (response.code() == 200) {
        val latest = response.body()
        Log.d(LOG_TAG, "latest: $latest")
        if (latest != null) {
            if (current == null) {
                // TODO: Fix this and return a non-nullable WeatherStation
                Log.e(LOG_TAG, "TODO: THIS SHOULD NEVER HAPPEN!!!")
                Timber.e("Update Station Not Found: $stationId")
                return null
            }
            val station = responseToStation(current, latest)
            Log.d(LOG_TAG, "station: $station")
            if (station == null) {
                Log.w(LOG_TAG, "New Data Rejected!")
                return current
            }
            dao.add(station)
            return station
        }
    }
    return current
}

suspend fun Context.updateStations(): List<WeatherStation> {
    val dao = StationDatabase.getInstance(this).stationDao()
    val stations = dao.getAll()
    Log.d(LOG_TAG, "stations.size: ${stations.size}")
    for (current in stations) {
        Log.d(LOG_TAG, "Updating Station ID: ${current.stationId}")
        val api = WeatherApi(this)
        val response = api.getLatest(current.stationId)
        Log.d(LOG_TAG, "response: ${response.code()} - isSuccessful: ${response.isSuccessful}")
        if (response.code() == 200) {
            val latest = response.body()
            Log.d(LOG_TAG, "latest: $latest")
            if (latest != null) {
                val station = responseToStation(current, latest)
                Log.d(LOG_TAG, "station: $station")
                if (station != null) {
                    if (station != current) {
                        Log.i(LOG_TAG, "UPDATED: ${station.stationId}")
                        dao.add(station)
                    } else {
                        Log.i(LOG_TAG, "NO CHANGE: ${station.stationId}")
                    }
                }
            }
        }
    }
    Log.i(LOG_TAG, "updateStations: DONE")
    return dao.getAll()
}


fun responseToStation(
    current: WeatherStation,
    latest: WeatherApi.ObservationResponse,
): WeatherStation? {
    if (
        latest.properties.temperature?.value == null &&
        latest.properties.dewpoint?.value == null &&
        latest.properties.relativeHumidity?.value == null
    ) {
        Log.w(LOG_TAG, "Rejecting New Data for Station: ${current.stationId}")
        Log.w(LOG_TAG, "latest: $latest")
        return null
    }
    return WeatherStation(
        stationId = current.stationId,
        active = current.active != false,
        name = current.name,
        elevation = current.elevation,
        coordinates = current.coordinates,

        station = latest.properties.station,
        timestamp = latest.properties.timestamp,
        rawMessage = latest.properties.rawMessage,
        textDescription = latest.properties.textDescription,
        icon = latest.properties.icon,

        barometricPressure = latest.properties.barometricPressure?.value,
        dewpoint = latest.properties.dewpoint?.value,
        relativeHumidity = latest.properties.relativeHumidity?.value,
        seaLevelPressure = latest.properties.seaLevelPressure?.value,
        temperature = latest.properties.temperature?.value,
        visibility = latest.properties.visibility?.value,
        windDirection = latest.properties.windDirection?.value,
        windSpeed = latest.properties.windSpeed?.value,
        windGust = latest.properties.windGust?.value,
    )
}

//fun getProperty(property: Value?): String? {
//    Log.d(LOG_TAG, "getProperty: $property")
//    if (property?.value != null) {
//        //val unitCode = property.value.toString().split(":")[1]
//        //Log.d(LOG_TAG, "unitCode: $unitCode")
//        val result = "${property.value} ${property.unitCode.toString().split(":")[1]}"
//        Log.d(LOG_TAG, "result: $result")
//        return result
//    }
//    Log.i(LOG_TAG, "result: NULL")
//    return null
//}

package org.cssnr.noaaweather.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.databinding.FragmentHomePagerBinding
import org.cssnr.noaaweather.db.StationDatabase
import org.cssnr.noaaweather.ui.stations.updateStations
import java.util.Locale

const val LOG_TAG = "Home"

class HomeFragment : Fragment() {

    private var _binding: FragmentHomePagerBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var homeStationAdapter: HomeStationAdapter
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomePagerBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "HomeFragment - onViewCreated: ${savedInstanceState?.size()}")

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        val appContext = requireContext()

        homeStationAdapter = HomeStationAdapter(this, emptyList())
        viewPager = binding.pager
        binding.pager.adapter = homeStationAdapter

        // TODO: Cleanup Logic for Updating Fragments and Setting Start Position...
        homeViewModel.data.observe(viewLifecycleOwner) { stations ->
            Log.d(LOG_TAG, "data.observe: stations.size: ${stations.size}")
            val activeStationPos = stations.indexOfFirst { it.active }
            Log.d(LOG_TAG, "data.observe: activeStationPos: $activeStationPos")
            if (homeViewModel.active.value != activeStationPos) {
                homeViewModel.active.value = activeStationPos
                homeViewModel.position.value = activeStationPos
                viewPager.setCurrentItem(activeStationPos, false)
            }
            homeStationAdapter.updateData(stations)
            Log.d(LOG_TAG, "homeViewModel.position.value: ${homeViewModel.position.value}")
            // TODO: This condition is always true now...
            if (homeViewModel.position.value != null) {
                Log.w(LOG_TAG, "UPDATING ALL FRAGMENTS: ${homeViewModel.position.value}")
                viewPager.setCurrentItem(homeViewModel.position.value ?: activeStationPos, false)
                childFragmentManager.fragments.forEach { fragment ->
                    if (fragment is UpdatableFragment) {
                        val itemId =
                            fragment.tag?.removePrefix("f")?.toLongOrNull() ?: return@forEach
                        //Log.d(LOG_TAG, "itemId: $itemId")
                        val index = homeStationAdapter.stations.indices.firstOrNull { index ->
                            //Log.d(LOG_TAG, "index: $index")
                            homeStationAdapter.getItemId(index) == itemId
                        } ?: return@forEach
                        fragment.updateData(homeStationAdapter.stations[index])
                    }
                }
            }
        }

        lifecycleScope.launch {
            val dao = StationDatabase.getInstance(appContext).stationDao()
            val stations = withContext(Dispatchers.IO) {
                dao.getAll()
            }
            Log.d(LOG_TAG, "lifecycleScope.launch: stations.size: ${stations.size}")
            if (!stations.isEmpty()) {
                if (homeViewModel.data.value != stations) {
                    Log.i(LOG_TAG, "SET NEW VIEW MODEL DATA")
                    homeViewModel.position.value = null
                    homeViewModel.data.value = stations
                } else {
                    Log.i(LOG_TAG, "MODEL DATA == STATIONS")
                }
            } else {
                Log.w(LOG_TAG, "STATIONS IS EMPTY")
            }
        }

        // TODO: Update Refresh for ViewPager2...
        binding.refreshDashboard.setOnClickListener { view ->
            Log.d(LOG_TAG, "binding.refreshDashboard.setOnClickListener")
            homeViewModel.position.value = binding.pager.currentItem
            Log.i(LOG_TAG, "position.value set to: ${binding.pager.currentItem}")
            binding.refreshDashboard.isEnabled = false
            binding.refreshDashboard.alpha = 0.3f
            lifecycleScope.launch {
                val stations = withContext(Dispatchers.IO) {
                    appContext.updateStations()
                }
                Log.d(LOG_TAG, "current: $stations")
                homeViewModel.data.value = stations
                withContext(Dispatchers.Main) {
                    Snackbar.make(view, "All Stations Refreshed.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .setAnchorView(R.id.refresh_dashboard).show()
                }
            }
        }
    }

    override fun onPause() {
        homeViewModel.position.value = binding.pager.currentItem
        Log.i(LOG_TAG, "onPause: position: ${homeViewModel.position.value}")
        super.onPause()
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
    //Log.d(LOG_TAG, "unit: $unit - value: $value - temp: $temp")
    val formatted = this.getString(R.string.format_temp, temp, unit)
    //Log.d(LOG_TAG, "formatted: $formatted")
    return formatted
}

fun shortCoords(coordinates: String): Pair<Double, Double> {
    val latitude = coordinates.split(",")[1].trim()
    val longitude = coordinates.split(",")[0].trim()
    val lat = String.format(Locale.US, "%.4f", latitude.toDouble())
    val lon = String.format(Locale.US, "%.4f", longitude.toDouble())
    return lat.toDouble() to lon.toDouble()
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

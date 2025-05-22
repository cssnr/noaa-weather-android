package org.cssnr.noaaweather.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.db.WeatherStation

class HomeStationAdapter(fragment: Fragment, var stations: List<WeatherStation>) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = stations.size

    override fun createFragment(position: Int): Fragment {
        val fragment = HomeStationFragment()
        //Log.d(LOG_TAG, "STEP 1 - createFragment: station: ${stations[position]}")
        fragment.arguments = Bundle().apply {
            putParcelable("station", stations[position])
        }
        return fragment
    }

    override fun getItemId(position: Int): Long {
        return stations[position].stationId.hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return stations.any { it.stationId.hashCode().toLong() == itemId }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<WeatherStation>) {
        Log.d(LOG_TAG, "updateData: ${newItems.size}")
        stations = newItems
        notifyDataSetChanged()
    }
}

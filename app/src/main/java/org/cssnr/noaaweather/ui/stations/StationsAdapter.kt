package org.cssnr.noaaweather.ui.stations

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.cssnr.noaaweather.MainActivity.Companion.LOG_TAG
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.db.WeatherStation

class StationsAdapter(
    private var items: List<WeatherStation>,
    private val onItemClick: (WeatherStation) -> Unit,
    private val onItemLongClick: (WeatherStation) -> Unit,
) :
    RecyclerView.Adapter<StationsAdapter.ViewHolder>() {

    private lateinit var context: Context

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemWrapper: LinearLayout = view.findViewById(R.id.item_wrapper)
        val propertiesName: TextView = view.findViewById(R.id.properties_name)
        val propertiesID: TextView = view.findViewById(R.id.properties_id)
        val propertiesElevation: TextView = view.findViewById(R.id.properties_elevation)
        val propertiesCoordinates: TextView = view.findViewById(R.id.properties_coordinates)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_station, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        //Log.d(LOG_TAG, "LOAD: $position - $data")

        // On Click
        holder.itemView.setOnClickListener {
            //val currentData = items[holder.bindingAdapterPosition]
            Log.i(LOG_TAG, "setOnClickListener: $position - $data")
            onItemClick(data)
        }
        holder.itemView.setOnLongClickListener {
            Log.i(LOG_TAG, "setOnLongClickListener: $position - $data")
            onItemLongClick(data)
            true
        }

        // Name and ID
        holder.propertiesName.text = items[position].name
        holder.propertiesID.text = items[position].stationId

        // Elevation
        holder.propertiesElevation.text = items[position].elevation

        // Coordinates
        holder.propertiesCoordinates.text = items[position].coordinates

        if (data.active) {
            holder.itemWrapper.setBackgroundResource(R.drawable.item_border_selected)
        } else {
            holder.itemWrapper.setBackgroundResource(R.drawable.item_border)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<WeatherStation>) {
        Log.i(LOG_TAG, "updateData: ${newItems.size}")
        items = newItems
        notifyDataSetChanged()
    }

//    @SuppressLint("NotifyDataSetChanged")
//    fun addItem(item: WeatherStation) {
//        Log.i(LOG_TAG, "addItem: $item")
//        items + item
//        Log.d(LOG_TAG, "getItemCount(): ${getItemCount()}")
//        notifyItemInserted(getItemCount())
//    }
}

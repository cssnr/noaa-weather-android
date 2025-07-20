package org.cssnr.noaaweather.ui.stations.add

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.cssnr.noaaweather.R
import org.cssnr.noaaweather.api.WeatherApi.ObservationStationsResponse

class AddDialogAdapter(
    private var items: List<ObservationStationsResponse.Feature>,
    private val onItemClick: (ObservationStationsResponse.Feature) -> Unit
) :
    RecyclerView.Adapter<AddDialogAdapter.ViewHolder>() {

    private lateinit var context: Context

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val propertiesName: TextView = view.findViewById(R.id.properties_name)
        val propertiesID: TextView = view.findViewById(R.id.properties_id)
        val propertiesElevation: TextView = view.findViewById(R.id.properties_elevation)
        val propertiesCoordinates: TextView = view.findViewById(R.id.properties_coordinates)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        Log.d(LOG_TAG, "$position - $data")

        // On Click
        holder.itemView.setOnClickListener {
            Log.i(LOG_TAG, "CLICK: $position - $data")
            onItemClick(data)
        }

        // Name and ID
        holder.propertiesName.text = items[position].properties.name
        holder.propertiesID.text = items[position].properties.stationIdentifier

        // Elevation
        val elevation = context.getString(
            R.string.elevation_unit,
            items[position].properties.elevation.value,
            items[position].properties.elevation.unitCode.split(":")[1]
        )
        holder.propertiesElevation.text = elevation

        // Coordinates
        val one = items[position].geometry.coordinates[0]
        val two = items[position].geometry.coordinates[1]
        val coordinates = context.getString(R.string.coordinates, one.toString(), two.toString())
        holder.propertiesCoordinates.text = coordinates
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: ObservationStationsResponse) {
        items = newItems.features
        notifyDataSetChanged()
    }
}

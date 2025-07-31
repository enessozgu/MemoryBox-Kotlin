package com.example.anikutusu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BonusAdapter(
    private val locationList: List<LocationModel>,
    private val onShowOnMapClicked: (LocationModel) -> Unit,
    private val onAddMemoryClicked: (LocationModel) -> Unit
) : RecyclerView.Adapter<BonusAdapter.LocationViewHolder>() {

    inner class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val descText: TextView = itemView.findViewById(R.id.descText)
        val showMapButton: Button = itemView.findViewById(R.id.showOnMapButton)
        val addMemoryButton: Button = itemView.findViewById(R.id.addMemoryButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_location, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locationList[position]
        holder.titleText.text = location.title
        holder.descText.text = location.description

        holder.showMapButton.setOnClickListener {
            onShowOnMapClicked(location)
        }

        holder.addMemoryButton.setOnClickListener {
            onAddMemoryClicked(location)
        }
    }

    override fun getItemCount(): Int = locationList.size
}

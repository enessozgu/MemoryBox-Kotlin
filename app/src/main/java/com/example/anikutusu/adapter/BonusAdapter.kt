package com.example.anikutusu.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.anikutusu.model.LocationModel
import com.example.anikutusu.R

// Adapter class for displaying a list of LocationModel items in a RecyclerView
class BonusAdapter(
    private val locationList: List<LocationModel>, // The list of location data to be shown
    private val onShowOnMapClicked: (LocationModel) -> Unit, // Callback for when "Show on Map" button is clicked
    private val onAddMemoryClicked: (LocationModel) -> Unit  // Callback for when "Add Memory" button is clicked
) : RecyclerView.Adapter<BonusAdapter.LocationViewHolder>() {

    // ViewHolder class that holds references to the views in each RecyclerView item
    inner class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.titleText) // Title text of the location
        val descText: TextView = itemView.findViewById(R.id.descText) // Description text of the location
        val showMapButton: Button = itemView.findViewById(R.id.showOnMapButton) // Button to show the location on the map
        val addMemoryButton: Button = itemView.findViewById(R.id.addMemoryButton) // Button to add a memory related to the location
    }

    // This function is called when a new ViewHolder needs to be created
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        // Inflate the layout for a single item in the RecyclerView
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_location, parent, false)
        return LocationViewHolder(view) // Return the newly created ViewHolder
    }

    // This function binds data to the views for each item in the RecyclerView
    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locationList[position] // Get the LocationModel for the current position

        // Set the title and description texts from the location data
        holder.titleText.text = location.title
        holder.descText.text = location.description

        // Set up the "Show on Map" button click event
        holder.showMapButton.setOnClickListener {
            onShowOnMapClicked(location) // Trigger the provided lambda function
        }

        // Set up the "Add Memory" button click event
        holder.addMemoryButton.setOnClickListener {
            onAddMemoryClicked(location) // Trigger the provided lambda function
        }
    }

    // Returns the total number of items in the list
    override fun getItemCount(): Int = locationList.size
}

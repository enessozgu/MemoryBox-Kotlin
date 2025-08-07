package com.example.anikutusu.model

// Data class that represents a location with title, description, and coordinates
data class LocationModel(
    val title: String,       // The title or name of the location
    val description: String, // A short description or note about the location
    val latitude: Double,    // Latitude coordinate (north-south position)
    val longitude: Double    // Longitude coordinate (east-west position)
)

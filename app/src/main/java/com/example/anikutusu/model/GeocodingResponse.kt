package com.example.anikutusu.model

// Data class representing the entire response from a Geocoding API
data class GeocodingResponse (
    val results: List<Result>, // List of geocoding results (could be multiple locations)
    val status: String         // Status of the API response (e.g., "OK", "ZERO_RESULTS")
)

// Data class representing a single geocoding result
data class Result(
    val formatted_address: String // Human-readable address string returned by the API
)

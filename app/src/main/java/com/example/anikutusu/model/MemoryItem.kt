package com.example.anikutusu.model

// Data class representing a memory item added by the user
data class MemoryItem(
    val id: String? = null,
    val userId: String = "",        // ID of the user who created the memory
    val text: String = "",          // Text content of the memory (description or note)
    val photoUrl: String? = null,   // Optional URL of the attached photo
    val audioUrl: String? = null,   // Optional URL of the attached audio recording
    val latitude: Double = 0.0,     // Latitude of the memory location
    val longitude: Double = 0.0,    // Longitude of the memory location
    val timestamp: Long? = null     // Time the memory was created (in milliseconds since epoch)
)

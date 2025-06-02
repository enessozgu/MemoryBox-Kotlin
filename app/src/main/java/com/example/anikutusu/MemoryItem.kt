package com.example.anikutusu.model

data class MemoryItem(
    val userId: String = "",
    val text: String = "",
    val photoUrl: String? = null,
    val audioUrl: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long? = null
)

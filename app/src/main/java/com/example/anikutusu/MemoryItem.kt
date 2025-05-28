package com.example.anikutusu.model

data class MemoryItem(
    val id: String = "",
    val text: String = "",
    val photoUrl: String? = null,
    val audioUrl: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

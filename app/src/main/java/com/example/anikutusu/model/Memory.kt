package com.example.anikutusu.model

data class Memory(
    val id: String? = null,        // DB key
    val title: String = "",        // Anı başlığı
    val description: String = "",  // Açıklama
    val imageUrl: String? = null,  // Fotoğraf linki (varsa)
    val timestamp: Long = System.currentTimeMillis(),
    val lat: Double? = null,       // Opsiyonel konum
    val lon: Double? = null
)

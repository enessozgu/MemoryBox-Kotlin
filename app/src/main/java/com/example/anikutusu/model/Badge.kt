package com.example.anikutusu.model

data class Badge(
    val id: String = "",             // DiffUtil için gerekli (b1, b2, …)
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val unlocked: Boolean = false,
    val progress: Int? = null        // hedefli rozetlerde % (0..100), yoksa null
)

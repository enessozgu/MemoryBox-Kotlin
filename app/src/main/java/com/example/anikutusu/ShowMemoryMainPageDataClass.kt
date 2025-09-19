package com.example.anikutusu

data class ShowMemoryMainPageDataClass(
    val id: String,          // RTDB key için gerekli (güvenli üretiyoruz)
    val imageUrl: String,
    val userName: String,
    val location: String,
    var isLiked: Boolean = false
)

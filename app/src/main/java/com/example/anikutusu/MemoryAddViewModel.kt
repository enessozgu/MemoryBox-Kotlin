package com.ornek.anikutusu.ui.viewmodel

import com.example.anikutusu.MemoryAddMode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.location.Location
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class MemoryAddViewModel : ViewModel() {

    // 1. Mod seçimi
    private val _selectedMode = MutableLiveData(MemoryAddMode.SERBEST_EKLE)
    val selectedMode: LiveData<MemoryAddMode> = _selectedMode

    fun toggleMode() {
        _selectedMode.value = when (_selectedMode.value) {
            MemoryAddMode.SERBEST_EKLE -> MemoryAddMode.YERINDE_EKLE
            else -> MemoryAddMode.SERBEST_EKLE
        }
    }

    // --- Rozet Sistemi ---

    companion object {
        const val ROZET_ILK_YERINDE = "badge_first_in_place"
        const val ROZET_GALATA_KULESI = "badge_galata_kulesi"
        const val ROZET_SULTANAHMET = "badge_sultanahmet"

        // Galata Kulesi koordinatları ve yarıçapı
        const val GALATA_KULESI_LAT = 41.0256
        const val GALATA_KULESI_LON = 28.9744
        const val GALATA_KULESI_RADIUS_METERS = 100f

        // Sultanahmet koordinatları ve yarıçapı
        const val SULTANAHMET_LAT = 41.0056
        const val SULTANAHMET_LON = 28.9768
        const val SULTANAHMET_RADIUS_METERS = 100f
    }

    private val _badges = MutableLiveData<Set<String>>(emptySet())
    val badges: LiveData<Set<String>> = _badges

    // Rozet ekleme fonksiyonu (Firestore ve local LiveData güncelleme)
    private fun addBadge(badgeName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userBadgesRef = db.collection("users").document(uid)

        // Firestore'da ilgili rozet alanını true yap
        userBadgesRef.set(mapOf(
            "badges.$badgeName" to true
        ), SetOptions.merge())

        // Local rozet setine ekle (yeni immutable set oluştur)
        _badges.value = _badges.value?.plus(badgeName)
    }

    // Galata Kulesi rozetini konuma göre ekle
    private fun addGalataKulesiBadgeIfNearby(userLat: Double, userLon: Double) {
        val userLocation = Location("").apply {
            latitude = userLat
            longitude = userLon
        }
        val galataLocation = Location("").apply {
            latitude = GALATA_KULESI_LAT
            longitude = GALATA_KULESI_LON
        }

        val distance = userLocation.distanceTo(galataLocation)
        if (distance <= GALATA_KULESI_RADIUS_METERS) {
            addBadge(ROZET_GALATA_KULESI)
        }
    }

    // Sultanahmet rozetini konuma göre ekle
    private fun addSultanAhmetBadgeIfNearby(userLat: Double, userLon: Double) {
        val userLocation = Location("").apply {
            latitude = userLat
            longitude = userLon
        }
        val sultanAhmetLocation = Location("").apply {
            latitude = SULTANAHMET_LAT
            longitude = SULTANAHMET_LON
        }

        val distance = userLocation.distanceTo(sultanAhmetLocation)
        if (distance <= SULTANAHMET_RADIUS_METERS) {
            addBadge(ROZET_SULTANAHMET)
        }
    }

    // Anı eklendiğinde çağrılacak fonksiyon: sadece Yerinde Ekle modunda rozet ekle
    fun addMemory(latitude: Double, longitude: Double) {
        if (_selectedMode.value == MemoryAddMode.YERINDE_EKLE) {
            // Yerinde ekle modunda, konuma göre rozetleri ekle
            addGalataKulesiBadgeIfNearby(latitude, longitude)
            addSultanAhmetBadgeIfNearby(latitude, longitude)
            addBadge(ROZET_ILK_YERINDE) // Yerinde ekle ilk rozet

            // Burada başka rozet kontrolleri de yapabilirsin
        } else {
            // Serbest mod için rozet yok veya farklı işlem yapılabilir
            onMemoryAdded()
        }
    }

    // Serbest modda anı eklendiğinde yapılacak işlemler (şimdilik boş)
     fun onMemoryAdded() {
        // Serbest modda rozet eklenmeyecek
    }

}

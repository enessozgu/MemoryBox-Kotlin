package com.ornek.anikutusu.ui.viewmodel

import com.example.anikutusu.MemoryAddMode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.location.Location

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

        const val SULTANAHMET_LAT = 41.0056
        const val SULTANAHMET_LON = 28.9768
        const val SULTANAHMET_RADIUS_METERS = 100f
    }

    // MutableLiveData'da Set<String> kullan, immutable set ile başlat
    private val _badges = MutableLiveData<Set<String>>(emptySet())
    val badges: LiveData<Set<String>> = _badges

    // Rozet ekleme fonksiyonu (yeni immutable set oluşturup ata)
    fun addBadge(badgeId: String) {
        val current = _badges.value ?: emptySet()
        if (!current.contains(badgeId)) {
            _badges.value = current + badgeId  // + operatörü yeni Set oluşturur
        }
    }

    fun addGalataKulesiBadgeIfNearby(userLat: Double, userLon: Double) {
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


    fun addSultanAhmetBadgeIfNearby(userLat: Double, userLon: Double) {
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
            addBadge(ROZET_SULTANAHMET)
        }
    }

    // Yerinde ekle modunda rozet ekleme
    fun onMemoryAddedInPlace(latitude: Double, longitude: Double) {
        // Burada istediğin konuma göre rozet kontrolü yapabilirsin
        addGalataKulesiBadgeIfNearby(latitude, longitude)
        addSultanAhmetBadgeIfNearby(latitude, longitude)
        addBadge(ROZET_ILK_YERINDE) // Örnek, istediğin başka rozetler de olabilir
    }


    fun onMemoryAdded() {
        // Parametresiz, serbest modda sadece anı ekleme işlemleri için
    }

}

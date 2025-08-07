package com.example.anikutusu.model

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.anikutusu.model.MemoryAddMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// ViewModel responsible for handling memory add logic and badge system
class MemoryAddViewModel : ViewModel() {

    // --- 1. Memory Add Mode Selection ---

    // Backing field for selected mode (default is SERBEST_EKLE)
    private val _selectedMode = MutableLiveData(MemoryAddMode.SERBEST_EKLE)
    val selectedMode: LiveData<MemoryAddMode> = _selectedMode

    // Toggles between SERBEST_EKLE and YERINDE_EKLE modes
    fun toggleMode() {
        _selectedMode.value = when (_selectedMode.value) {
            MemoryAddMode.SERBEST_EKLE -> MemoryAddMode.YERINDE_EKLE
            else -> MemoryAddMode.SERBEST_EKLE
        }
    }

    // --- Badge System ---

    companion object {
        // Badge keys used for Firestore and local UI
        const val ROZET_ILK_YERINDE = "badge_first_in_place"
        const val ROZET_GALATA_KULESI = "badge_galata_kulesi"
        const val ROZET_SULTANAHMET = "badge_sultanahmet"

        // Coordinates and radius for Galata Tower
        const val GALATA_KULESI_LAT = 41.0256
        const val GALATA_KULESI_LON = 28.9744
        const val GALATA_KULESI_RADIUS_METERS = 100f

        // Coordinates and radius for Sultanahmet
        const val SULTANAHMET_LAT = 41.0056
        const val SULTANAHMET_LON = 28.9768
        const val SULTANAHMET_RADIUS_METERS = 100f
    }

    // LiveData to hold the currently earned badges
    private val _badges = MutableLiveData<Set<String>>(emptySet())
    val badges: LiveData<Set<String>> = _badges

    // Adds a badge both to Firestore and to the local badge set
    private fun addBadge(badgeName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userBadgesRef = db.collection("users").document(uid)

        // Set the badge field to true in Firestore under "badges" map
        userBadgesRef.set(mapOf(
            "badges.$badgeName" to true
        ), SetOptions.merge())

        // Add the badge to local LiveData set (creates new immutable set)
        _badges.value = _badges.value?.plus(badgeName)
    }

    // Check if user is near Galata Tower, and add the badge if so
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

    // Check if user is near Sultanahmet, and add the badge if so
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

    // Called when a memory is added
    // Only triggers badge checks in YERINDE_EKLE mode
    fun addMemory(latitude: Double, longitude: Double) {
        if (_selectedMode.value == MemoryAddMode.YERINDE_EKLE) {
            // Check location-based badges
            addGalataKulesiBadgeIfNearby(latitude, longitude)
            addSultanAhmetBadgeIfNearby(latitude, longitude)

            // Always give the first in-place badge
            addBadge(ROZET_ILK_YERINDE)

            // Additional badge checks can be added here
        } else {
            // No badges for free mode
            onMemoryAdded()
        }
    }

    // Optional logic when memory is added in SERBEST_EKLE mode
    fun onMemoryAdded() {
        // Currently does nothing for free add mode
    }

}

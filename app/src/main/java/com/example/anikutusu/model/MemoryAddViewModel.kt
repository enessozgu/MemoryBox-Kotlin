package com.example.anikutusu.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.anikutusu.data.BadgeRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

// ViewModel: rozet mantığı (ilk anı, 10 anı vb.)
class MemoryAddViewModel : ViewModel() {

    // --- Mod seçimi (UI) ---
    private val _selectedMode = MutableLiveData(MemoryAddMode.SERBEST_EKLE)
    val selectedMode: LiveData<MemoryAddMode> = _selectedMode
    fun toggleMode() {
        _selectedMode.value =
            if (_selectedMode.value == MemoryAddMode.SERBEST_EKLE) MemoryAddMode.YERINDE_EKLE
            else MemoryAddMode.SERBEST_EKLE
    }

    // --- Rozet ID'leri (Realtime DB badgeId) ---
    companion object {
        const val B1_FIRST_MEMORY = "b1"
        const val B2_TEN_MEMORY   = "b2"

        // İstersen konum rozetleri de kullan:
        const val B_GALATA        = "b_galata"
        const val B_SULTANAHMET   = "b_sultan"

        const val GALATA_KULESI_LAT = 41.0256
        const val GALATA_KULESI_LON = 28.9744
        const val GALATA_KULESI_RADIUS_METERS = 100f

        const val SULTANAHMET_LAT = 41.0056
        const val SULTANAHMET_LON = 28.9768
        const val SULTANAHMET_RADIUS_METERS = 100f
    }

    // UI’da kazanılan rozetleri göstermek istersen:
    private val _badges = MutableLiveData<Set<String>>(emptySet())
    val badges: LiveData<Set<String>> = _badges

    // Realtime DB & Auth
    private val db: DatabaseReference =
        FirebaseDatabase.getInstance("https://anikutusuapp-default-rtdb.firebaseio.com/").reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Users/{userName}Data node cache (şema kullanıcı adı tabanlı)
    @Volatile private var cachedUserNode: DatabaseReference? = null

    private fun withUserNode(
        onReady: (DatabaseReference) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        cachedUserNode?.let { onReady(it); return }

        val email = auth.currentUser?.email
            ?: return onError(IllegalStateException("User email not available"))

        db.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val match = s.children.firstOrNull { child ->
                    val key = child.key ?: return@firstOrNull false
                    if (!key.endsWith("Data")) return@firstOrNull false
                    child.child("userMail").getValue(String::class.java)
                        ?.equals(email, ignoreCase = true) == true
                }?.ref

                if (match == null) onError(IllegalStateException("User node not found"))
                else { cachedUserNode = match; onReady(match) }
            }
            override fun onCancelled(e: DatabaseError) = onError(e.toException())
        })
    }

    // Rozet repo (Realtime)
    private val badgeRepo = BadgeRepository(
        db = db,
        auth = auth
    )

    /**
     * Fragment, anıyı Firestore’a BAŞARIYLA kaydettikten SONRA burayı çağırır.
     * Realtime DB'de Users/{userName}Data/Memories altındaki toplam sayıya göre rozet güncellenir.
     * (Fragment, Realtime’a da küçük bir “iz” yazar; toplam sayım ondan gelir.)
     */
    fun onMemoryPersisted(userLat: Double? = null, userLon: Double? = null) {
        withUserNode(onReady = { userNode ->
            val memoriesRef = userNode.child("Memories")

            memoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val count = snap.childrenCount

                    // 1) İlk anıysa b1'i aç
                    if (count == 1L) {
                        badgeRepo.unlockBadge(B1_FIRST_MEMORY) { _, _ -> }
                        _badges.value = _badges.value?.plus(B1_FIRST_MEMORY)
                    }

                    // 2) b2 progress: 10 anıda %100
                    val p = (count.coerceAtMost(10).toInt() * 10)
                    badgeRepo.setBadgeProgress(B2_TEN_MEMORY, p, autoUnlockAt100 = true) { _, _ -> }

                    // 3) (Opsiyonel) konum rozetleri:
                    if (userLat != null && userLon != null) {
                        addGalataIfNearby(userLat, userLon)
                        addSultanahmetIfNearby(userLat, userLon)
                    }
                }
                override fun onCancelled(error: DatabaseError) { /* no-op */ }
            })
        })
    }

    // --- Konum rozetleri (opsiyonel) ---
    private fun addGalataIfNearby(lat: Double, lon: Double) {
        val d = distanceMeters(lat, lon, GALATA_KULESI_LAT, GALATA_KULESI_LON)
        if (d <= GALATA_KULESI_RADIUS_METERS) {
            badgeRepo.unlockBadge(B_GALATA) { _, _ -> }
            _badges.value = _badges.value?.plus(B_GALATA)
        }
    }
    private fun addSultanahmetIfNearby(lat: Double, lon: Double) {
        val d = distanceMeters(lat, lon, SULTANAHMET_LAT, SULTANAHMET_LON)
        if (d <= SULTANAHMET_RADIUS_METERS) {
            badgeRepo.unlockBadge(B_SULTANAHMET) { _, _ -> }
            _badges.value = _badges.value?.plus(B_SULTANAHMET)
        }
    }
    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val a = android.location.Location("").apply { latitude = lat1; longitude = lon1 }
        val b = android.location.Location("").apply { latitude = lat2; longitude = lon2 }
        return a.distanceTo(b)
    }
}

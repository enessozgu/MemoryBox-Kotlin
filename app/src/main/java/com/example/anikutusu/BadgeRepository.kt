// BadgeRepository.kt (username-based schema compatible)
package com.example.anikutusu.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BadgeRepository(
    // İstersen sabit URL ver: FirebaseDatabase.getInstance("https://anikutusuapp-default-rtdb.firebaseio.com/").reference
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // Bulduktan sonra tekrar tekrar aramamak için cache
    @Volatile private var cachedUserNode: DatabaseReference? = null

    /**
     * Users altındaki {userName}Data düğümünü çözer.
     * Kriter: child.userMail == auth.email (case-insensitive)
     * Bulunca cache'ler ve callback'e verir.
     */
    private fun withUserNode(
        onReady: (DatabaseReference) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        cachedUserNode?.let { onReady(it); return }

        val email = auth.currentUser?.email
            ?: return onError(IllegalStateException("User email not available"))

        db.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var matchRef: DatabaseReference? = null
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    if (!key.endsWith("Data")) continue
                    val mail = child.child("userMail").getValue(String::class.java)
                    if (mail?.equals(email, ignoreCase = true) == true) {
                        matchRef = child.ref
                        break
                    }
                }
                if (matchRef == null) {
                    onError(IllegalStateException("User node not found for $email"))
                } else {
                    cachedUserNode = matchRef
                    onReady(matchRef!!)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                onError(error.toException())
            }
        })
    }

    /** İlk rozet setini yazar (hesap oluşturma/ilk girişte 1 kez çağır). */
    fun setInitialBadges(
        onComplete: (Boolean, Exception?) -> Unit = {_,_->}
    ) {
        withUserNode(onReady = { node ->
            val badgesRef = node.child("badges")
            val initialBadges = mapOf(
                "b1" to mapOf(
                    "name" to "İlk Anı",
                    "description" to "İlk anını paylaştığın için",
                    "iconUrl" to "https://example.com/icons/ilk-ani.png",
                    "unlocked" to false,
                    "progress" to null
                ),
                "b2" to mapOf(
                    "name" to "10 Anı",
                    "description" to "10 anı kaydet!",
                    "iconUrl" to "https://example.com/icons/10-ani.png",
                    "unlocked" to false,
                    "progress" to 0
                )
            )
            badgesRef.updateChildren(initialBadges) { err, _ ->
                onComplete(err == null, err?.toException())
            }
        }, onError = { e -> onComplete(false, e) })
    }

    /** Rozeti kilitsiz yapar, isterse progress'i sıfırlar (null). */
    fun unlockBadge(
        badgeId: String,
        onComplete: (Boolean, Exception?) -> Unit = {_,_->}
    ) {
        withUserNode(onReady = { node ->
            val ref = node.child("badges").child(badgeId)
            val updates = mapOf("unlocked" to true, "progress" to null)
            ref.updateChildren(updates) { err, _ ->
                onComplete(err == null, err?.toException())
            }
        }, onError = { e -> onComplete(false, e) })
    }

    /** Progress set eder; 100'e ulaşırsa otomatik kilit açabilir. */
    fun setBadgeProgress(
        badgeId: String,
        newProgress: Int,
        autoUnlockAt100: Boolean = true,
        onComplete: (Boolean, Exception?) -> Unit = {_,_->}
    ) {
        val safeProgress = newProgress.coerceIn(0, 100)
        withUserNode(onReady = { node ->
            val ref = node.child("badges").child(badgeId)
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val map = currentData.value as? Map<*, *>
                    val unlocked = map?.get("unlocked") as? Boolean ?: false
                    if (unlocked) return Transaction.success(currentData)

                    val result = mutableMapOf<String, Any?>(
                        "progress" to safeProgress,
                        "unlocked" to (autoUnlockAt100 && safeProgress >= 100)
                    )
                    // Alanlar yoksa koruyucu set
                    result.putIfAbsent("name", map?.get("name") ?: "")
                    result.putIfAbsent("description", map?.get("description") ?: "")
                    result.putIfAbsent("iconUrl", map?.get("iconUrl") ?: "")

                    currentData.value = result
                    return Transaction.success(currentData)
                }
                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    onComplete(error == null && committed, error?.toException())
                }
            })
        }, onError = { e -> onComplete(false, e) })
    }

    /** Rozetleri canlı dinle (UI senk). */
    fun observeBadges(
        onChanged: (Map<String, Any?>) -> Unit
    ): ValueEventListener? {
        var listener: ValueEventListener? = null
        withUserNode(onReady = { node ->
            val ref = node.child("badges")
            listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onChanged(snapshot.value as? Map<String, Any?> ?: emptyMap())
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(listener!!)
        }, onError = { /* log e */ })
        return listener
    }

    fun removeObserver(listener: ValueEventListener?) {
        val node = cachedUserNode ?: return
        listener ?: return
        node.child("badges").removeEventListener(listener)
    }
}

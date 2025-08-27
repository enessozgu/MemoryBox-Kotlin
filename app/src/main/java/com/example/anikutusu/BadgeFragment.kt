package com.example.anikutusu.ui.badges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.anikutusu.databinding.FragmentBadgeBinding
import com.example.anikutusu.model.Badge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BadgeFragment : Fragment() {

    private var _binding: FragmentBadgeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BadgeAdapter
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private var badgesListener: ValueEventListener? = null
    private var badgesRef: DatabaseReference? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBadgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = BadgeAdapter { badge -> showBadgeDetails(badge) }

        binding.badgesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.badgesRecyclerView.adapter = adapter

        binding.badgesTitle.text = "Rozetlerim"
        binding.badgeStats.text = ""
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE

        // 1) Mevcut şemaya uygun kullanıcı düğümünü çöz (Users/{userKey}Data)
        resolveCurrentUserDataNode { userDataNode ->
            if (userDataNode == null) {
                binding.progressBar.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
                binding.emptyView.text = "Kullanıcı bulunamadı."
                return@resolveCurrentUserDataNode
            }
            // 2) Rozetleri oku (realtime veya single-shot)
            attachBadgesListener(userDataNode.child("badges"))
        }
    }

    /**
     * Şemaya göre kullanıcıyı bul:
     * - Users altında gezip child.userMail == auth.email olan ilk {userKey}Data düğümünü seçer.
     * Not: Kullanıcı sayısı büyükse server-side query yok; full scan var. İstersen yanına bir indeksleme ekleyebilirsin.
     */
    private fun resolveCurrentUserDataNode(callback: (DatabaseReference?) -> Unit) {
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email.isNullOrBlank()) {
            callback(null); return
        }

        // Users’ı bir kere çek, içinde userMail == email olan ...Data düğümünü bul
        db.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var match: DatabaseReference? = null
                for (child in snapshot.children) {
                    // Sadece ...Data düğümlerini değerlendir (senin şeman böyle)
                    val key = child.key ?: continue
                    if (!key.endsWith("Data")) continue

                    val mail = child.child("userMail").getValue(String::class.java)
                    if (mail.equals(email, ignoreCase = true)) {
                        match = child.ref
                        break
                    }
                }
                callback(match)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    /**
     * Realtime dinleyici. Tek seferlik istiyorsan addListenerForSingleValueEvent kullan.
     */
    private fun attachBadgesListener(ref: DatabaseReference) {
        // Önce eski dinleyiciyi kaldır (tekrar girişleri önlemek için)
        badgesListener?.let { badgesRef?.removeEventListener(it) }
        badgesRef = ref

        badgesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.map { snap ->
                    val id = snap.key ?: ""
                    val name = snap.child("name").getValue(String::class.java) ?: ""
                    val description = snap.child("description").getValue(String::class.java) ?: ""
                    val iconUrl = snap.child("iconUrl").getValue(String::class.java) ?: ""

                    // Realtime DB number -> Long gelebilir; güvenli dönüştür
                    val unlockedAny = snap.child("unlocked").value
                    val unlocked = when (unlockedAny) {
                        is Boolean -> unlockedAny
                        is String -> unlockedAny.equals("true", ignoreCase = true)
                        is Number -> unlockedAny.toInt() != 0
                        else -> false
                    }

                    val progressAny = snap.child("progress").value
                    val progress: Int? = when (progressAny) {
                        null -> null
                        is Number -> progressAny.toInt()
                        is String -> progressAny.toIntOrNull()
                        else -> null
                    }

                    Badge(
                        id = id,
                        name = name,
                        description = description,
                        iconUrl = iconUrl,
                        unlocked = unlocked,
                        progress = progress
                    )
                }.sortedWith(compareBy<Badge>({ !it.unlocked }, { it.id })) // önce unlocked’lar

                binding.progressBar.visibility = View.GONE
                if (list.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.emptyView.text = "Henüz rozet yok."
                } else {
                    binding.emptyView.visibility = View.GONE
                }

                adapter.submitList(list)

                val opened = list.count { it.unlocked }
                val locked = list.size - opened
                binding.badgeStats.text = "Açık: $opened  •  Kilitli: $locked"
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
                binding.emptyView.text = "Rozetler yüklenemedi."
            }
        }

        // Realtime güncelleme (canlı değişsin)
        ref.addValueEventListener(badgesListener as ValueEventListener)
        // Tek seferlik istiyorsan şunu kullan:
        // ref.addListenerForSingleValueEvent(badgesListener as ValueEventListener)
    }

    private fun showBadgeDetails(badge: Badge) {
        val sheet = BadgeDetailBottomSheet.newInstance(
            badge.id, badge.name, badge.description, badge.iconUrl, badge.unlocked, badge.progress
        )
        sheet.show(childFragmentManager, "badge_detail")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // memory leak / duplicate listener önle
        badgesListener?.let { l ->
            badgesRef?.removeEventListener(l)
        }
        badgesListener = null
        badgesRef = null
        _binding = null
    }
}

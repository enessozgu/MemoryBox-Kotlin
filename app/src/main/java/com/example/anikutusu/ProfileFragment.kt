package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.anikutusu.databinding.FragmentProfileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.storage

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val usersRef by lazy { FirebaseDatabase.getInstance().reference.child("Users") }

    // Dinleyicileri sonradan kaldırmak için referans tutalım
    private var followersListener: ValueEventListener? = null
    private var followingListener: ValueEventListener? = null

    private fun String.sanitizeKey() = this.replace(".", "_")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ---- Profil görseli (senin mevcut kodun) ----
        val storageRef = Firebase.storage.reference
            .child("images/f348f647-eba8-4d8a-a6a3-682fc4622783.jpg")

        storageRef.downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(this@ProfileFragment)
                    .load(uri)
                    .circleCrop()
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(binding.imgProfile)
            }
            .addOnFailureListener {
                binding.imgProfile.setImageResource(android.R.drawable.ic_menu_report_image)
            }

        // ---- Kullanıcı adı / key hazırlığı
        // displayName yoksa e-postanın @ öncesini alalım
        val displayName = auth.currentUser?.displayName?.trim().orEmpty()
        val email = auth.currentUser?.email.orEmpty()
        val fallbackName = email.substringBefore('@')
        val profileUserName = (if (displayName.isNotEmpty()) displayName else fallbackName).trim()
        val profileKey = "${profileUserName}Data".sanitizeKey()

        // (İstersen ekranda göster)
        // binding.txtUserName.text = profileUserName

        // ---- Takipçi / Takip edilen sayıları (Realtime) ----
        val followersRef = usersRef.child(profileKey).child("userFollowerList")
        val followingRef = usersRef.child(profileKey).child("userFollowingList")

        followersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.followersnumbers.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) { /* loglayabilirsin */ }
        }
        followingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.followednumbers.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) { /* loglayabilirsin */ }
        }

        followersRef.addValueEventListener(followersListener as ValueEventListener)
        followingRef.addValueEventListener(followingListener as ValueEventListener)

        // ---- Sayaçlara tıklayınca FollowInfo'ya git ----
        fun goFollowInfo(initialTab: String) {
            val bundle = Bundle().apply {
                putString("username", profileUserName) // hangi profilin listesi açılacak
                putString("initialTab", initialTab)    // "followers" | "following"
            }
            findNavController().navigate(R.id.action_global_followInfoFragment, bundle)
        }

        binding.followersnumbers.setOnClickListener { goFollowInfo("followers") } // Takipçi listesi
        binding.followednumbers.setOnClickListener { goFollowInfo("following") } // Takip edilen listesi
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Dinleyicileri kaldır (memory leak ve eski view'a yazmayı önler)
        val displayName = auth.currentUser?.displayName?.trim().orEmpty()
        val email = auth.currentUser?.email.orEmpty()
        val fallbackName = email.substringBefore('@')
        val profileUserName = (if (displayName.isNotEmpty()) displayName else fallbackName).trim()
        val profileKey = "${profileUserName}Data".sanitizeKey()

        followersListener?.let {
            FirebaseDatabase.getInstance().reference
                .child("Users").child(profileKey).child("userFollowerList")
                .removeEventListener(it)
        }
        followingListener?.let {
            FirebaseDatabase.getInstance().reference
                .child("Users").child(profileKey).child("userFollowingList")
                .removeEventListener(it)
        }

        _binding = null
    }
}

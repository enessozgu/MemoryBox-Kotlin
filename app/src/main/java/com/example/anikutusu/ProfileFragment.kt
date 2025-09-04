package com.example.anikutusu

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.anikutusu.databinding.FragmentProfileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.storage

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val usersRef by lazy { FirebaseDatabase.getInstance().reference.child("Users") }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Realtime dinleyicileri
    private var followersListener: ValueEventListener? = null
    private var followingListener: ValueEventListener? = null

    private fun String.sanitizeKey() = this.replace(".", "_")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        // --- Profil görseli (Storage) ---
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

        // --- Kullanıcı adı / key ---
        val displayName = auth.currentUser?.displayName?.trim().orEmpty()
        val email = auth.currentUser?.email.orEmpty()
        val fallbackName = email.substringBefore('@')
        val profileUserName = (if (displayName.isNotEmpty()) displayName else fallbackName).trim()
        val profileKey = "${profileUserName}Data".sanitizeKey()

        binding.userName.text = profileUserName

        // --- Realtime sayaçlar ---
        val followersRef = usersRef.child(profileKey).child("userFollowerList")
        val followingRef = usersRef.child(profileKey).child("userFollowingList")

        followersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.followersnumbers.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) { /* log optional */ }
        }
        followingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.followednumbers.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) { /* log optional */ }
        }

        followersRef.addValueEventListener(followersListener as ValueEventListener)
        followingRef.addValueEventListener(followingListener as ValueEventListener)

        // --- FollowInfo'ya geçiş (global action + initialTab) ---
        fun goFollowInfo(initialTab: String) {
            val bundle = Bundle().apply {
                putString("username", profileUserName)               // Hangi profil?
                putString("initialTab", initialTab)                  // "followers" | "following"
                putString("followersCount", binding.followersnumbers.text.toString())
                putString("followingCount", binding.followednumbers.text.toString())
            }
            findNavController().navigate(R.id.action_global_followInfoFragment, bundle)
        }

        binding.followersnumbers.setOnClickListener { goFollowInfo("followers") }
        binding.followednumbers.setOnClickListener { goFollowInfo("following") }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Dinleyicileri kaldır
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

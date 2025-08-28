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

    private var currentUserName: String = ""
    private var currentFollowersCount: Long = 0
    private var currentFollowingCount: Long = 0
    private var headerListener: ValueEventListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        // --- Profil görseli ---
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

        // --- Header verilerini yükle (realtime) ---
        attachMyProfileHeaderListener()

        // --- Takip info sayfasına geçiş + bundle gönder ---
        val openFollowInfo: (View) -> Unit = {
            val bundle = Bundle().apply {
                putString("username", currentUserName)
                putString("followersCount", currentFollowersCount.toString())
                putString("followingCount", currentFollowingCount.toString())
            }
            findNavController().navigate(
                R.id.action_global_followInfoFragment,
                bundle
            )
        }
        binding.followersnumbers.setOnClickListener(openFollowInfo)
        binding.followednumbers.setOnClickListener(openFollowInfo)
    }

    private fun attachMyProfileHeaderListener() {
        val email = auth.currentUser?.email ?: return
        val query = usersRef.orderByChild("userMail").equalTo(email)

        headerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val node = snapshot.children.firstOrNull()
                if (node == null) {
                    // fallback
                    currentUserName = email.substringBefore("@")
                    currentFollowingCount = 0
                    currentFollowersCount = 0

                    binding.userName.text = currentUserName
                    binding.followednumbers.text = "0"
                    binding.followersnumbers.text = "0"
                    return
                }

                // userName
                currentUserName = node.child("userName").getValue(String::class.java).orEmpty()
                binding.userName.text = currentUserName.ifEmpty { email.substringBefore("@") }

                // following count (dal adı: userFollowingList)
                currentFollowingCount = node.child("userFollowingList").childrenCount
                binding.followednumbers.text = currentFollowingCount.toString()

                // followers count (dal adı: userFollowerList)
                currentFollowersCount = node.child("userFollowerList").childrenCount
                binding.followersnumbers.text = currentFollowersCount.toString()
            }

            override fun onCancelled(error: DatabaseError) { /* no-op */ }
        }

        query.addValueEventListener(headerListener as ValueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Dinleyiciyi temizle
        val email = auth.currentUser?.email
        if (email != null && headerListener != null) {
            usersRef.orderByChild("userMail").equalTo(email)
                .removeEventListener(headerListener!!)
        }
        _binding = null
    }
}

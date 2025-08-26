package com.example.anikutusu

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import com.example.anikutusu.databinding.FragmentOtherProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OtherProfileFragment : Fragment() {

    private lateinit var binding: FragmentOtherProfileBinding
    private val db by lazy { FirebaseDatabase.getInstance().reference.child("Users") }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private fun String.sanitizeKey() = this.replace(".", "_")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentOtherProfileBinding.inflate(inflater, container, false)

        // --- 1) Argümandan kullanıcı adını oku; önce bunu yap ---
        val profileUserName = (arguments?.getString("username") ?: "").trim()
        binding.userName.text = profileUserName

        // --- 2) Key’leri doğru oluştur ---
        val profileKey = "${profileUserName}Data".sanitizeKey()
        val loginUserName = (auth.currentUser?.displayName ?: "").trim()
        val loginKey = "${loginUserName}Data".sanitizeKey()

        // --- 3) Takip et (iki tek satır) ---
        binding.followButton.setOnClickListener {
            if (profileUserName.isEmpty() || loginUserName.isEmpty()) return@setOnClickListener
            db.child(loginKey).child("userFollowingList").child("${loginUserName}Followed${profileUserName}".sanitizeKey()).setValue(profileUserName)
            db.child(profileKey).child("userFollowerList").child("${profileUserName}Follower${loginUserName}".sanitizeKey()).setValue(loginUserName)
        }

        // --- 4) SAYILAR (Realtime güncellenir) ---
        // Takipçi (followers) -> "Takipçi" yazan TextView
        db.child(profileKey).child("userFollowerList").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) { binding.followersnumbers.text = snap.childrenCount.toString() }
            override fun onCancelled(err: DatabaseError) { Log.e("followersRef", err.message) }
        })

        // Takip Edilen (following) -> "Takip Edilen" TextView
        db.child(profileKey).child("userFollowingList").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) { binding.followednumbers.text = snap.childrenCount.toString() }
            override fun onCancelled(err: DatabaseError) { Log.e("followingRef", err.message) }
        })

        return binding.root
    }










}

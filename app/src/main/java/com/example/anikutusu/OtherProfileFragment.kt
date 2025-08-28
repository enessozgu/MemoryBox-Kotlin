package com.example.anikutusu

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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

        // 1) Argüman: görüntülenen profilin kullanıcı adı
        val profileUserName = (arguments?.getString("username") ?: "").trim()
        binding.userName.text = profileUserName

        // 2) Key’ler
        val profileKey = "${profileUserName}Data".sanitizeKey()
        val loginUserName = (auth.currentUser?.displayName ?: "").trim()
        val loginKey = "${loginUserName}Data".sanitizeKey()

        // 3) Takip et
        binding.followButton.setOnClickListener {
            if (profileUserName.isEmpty() || loginUserName.isEmpty()) return@setOnClickListener
            db.child(loginKey).child("userFollowingList")
                .child("${loginUserName}Followed${profileUserName}".sanitizeKey())
                .setValue(profileUserName)

            db.child(profileKey).child("userFollowerList")
                .child("${profileUserName}Follower${loginUserName}".sanitizeKey())
                .setValue(loginUserName)
        }

        // 4) Sayılar (Realtime)
        db.child(profileKey).child("userFollowerList").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) { binding.followersnumbers.text = snap.childrenCount.toString() }
            override fun onCancelled(err: DatabaseError) { Log.e("followersRef", err.message) }
        })
        db.child(profileKey).child("userFollowingList").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) { binding.followednumbers.text = snap.childrenCount.toString() }
            override fun onCancelled(err: DatabaseError) { Log.e("followingRef", err.message) }
        })

        // 5) >>> Sayaçlara tıklayınca FollowInfo’ya git
        fun goFollowInfo(initialTab: String) {
            val bundle = Bundle().apply {
                putString("username", profileUserName)   // Hangi profilin listesi açılacak
                putString("initialTab", initialTab)      // "followers" | "following"
            }
            findNavController().navigate(R.id.action_global_followInfoFragment, bundle)
        }

        binding.followersnumbers.setOnClickListener { goFollowInfo("followers") } // Takipçi
        binding.followednumbers.setOnClickListener { goFollowInfo("following") } // Takip edilen

        return binding.root
    }
}

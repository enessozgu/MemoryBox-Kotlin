package com.example.anikutusu

import android.os.Bundle
import android.renderscript.ScriptGroup.Binding
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.anikutusu.adapter.FollowInfoAdapter
import com.example.anikutusu.databinding.FragmentFollowersInfoBinding
import com.example.anikutusu.databinding.FragmentFollowingInfoBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FollowingInfoFragment : Fragment() {

    private lateinit var binding: FragmentFollowersInfoBinding // aynı layout’u kullanıyorsan
    private lateinit var adapter: FollowInfoAdapter
    private val list = ArrayList<UserDataClass>()

    companion object {
        fun newInstance(username: String) = FollowingInfoFragment().apply {
            arguments = bundleOf("username" to username)
        }
    }

    private fun String.sanitizeKey() = this.replace(".", "_")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFollowersInfoBinding.inflate(inflater, container, false)

        adapter = FollowInfoAdapter(requireContext(), list)
        binding.rvInfo.apply {
            setHasFixedSize(true)
            layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
            this.adapter = this@FollowingInfoFragment.adapter
        }

        val username = (requireArguments().getString("username") ?: "").trim()
        if (username.isNotEmpty()) loadFollowing(username)

        return binding.root
    }

    private fun loadFollowing(ownerUsername: String) {
        val db = FirebaseDatabase.getInstance().reference.child("Users")
        val ownerKey = "${ownerUsername}Data".sanitizeKey()

        // Users/{owner}Data/userFollowingList içerisinde: key: "{owner}Followed{target}", value: "target"
        db.child(ownerKey).child("userFollowingList")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val followingUsernames = snap.children.mapNotNull { it.getValue(String::class.java) }
                    if (followingUsernames.isEmpty()) {
                        list.clear()
                        adapter.notifyDataSetChanged()
                        return
                    }
                    fetchUserCards(db, followingUsernames)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchUserCards(usersRefRoot: DatabaseReference, usernames: List<String>) {
        list.clear()
        var left = usernames.size
        usernames.forEach { uname ->
            val key = "${uname}Data".sanitizeKey()
            usersRefRoot.child(key)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(s: DataSnapshot) {
                        val mail = s.child("userMail").getValue(String::class.java) ?: ""
                        list.add(UserDataClass(uname, mail, R.drawable.images))
                        if (--left == 0) adapter.notifyDataSetChanged()
                    }
                    override fun onCancelled(error: DatabaseError) { if (--left == 0) adapter.notifyDataSetChanged() }
                })
        }
    }
}

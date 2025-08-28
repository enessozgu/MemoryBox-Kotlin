package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.anikutusu.databinding.FragmentFollowInfoBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.*

class FollowInfoFragment : Fragment() {

    private lateinit var binding: FragmentFollowInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentFollowInfoBinding.inflate(inflater, container, false)

        val profileUserName = (arguments?.getString("username") ?: "").trim()
        val profileFollowersCount = "${(arguments?.getString("followersCount") ?: "0").trim()} Takipçi"
        val profileFollowedCount  = "${(arguments?.getString("followingCount") ?: "0").trim()} Takip"

        // ViewPager2: username'i çocuklara ver
        binding.viewpager.adapter = MyViewPagerAdapter(this, profileUserName)

        TabLayoutMediator(binding.tablayout, binding.viewpager) { tab, position ->
            tab.text = if (position == 0) profileFollowersCount else profileFollowedCount
        }.attach()

        // Canlı sayılar (suffix KORUNUR)
        if (profileUserName.isNotEmpty()) {
            val profileKey = "${profileUserName}Data".replace(".", "_")
            val usersRef = FirebaseDatabase.getInstance().reference.child("Users")

            usersRef.child(profileKey).child("userFollowerList")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(s: DataSnapshot) {
                        binding.tablayout.getTabAt(0)?.text = "${s.childrenCount} Takipçi"
                    }
                    override fun onCancelled(e: DatabaseError) {}
                })

            usersRef.child(profileKey).child("userFollowingList")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(s: DataSnapshot) {
                        binding.tablayout.getTabAt(1)?.text = "${s.childrenCount} Takip"
                    }
                    override fun onCancelled(e: DatabaseError) {}
                })
        }
        return binding.root
    }

    private class MyViewPagerAdapter(
        parent: Fragment,
        private val username: String
    ) : FragmentStateAdapter(parent) {
        override fun getItemCount() = 2
        override fun createFragment(position: Int): Fragment {
            return if (position == 0)
                FollowersInfoFragment.newInstance(username)
            else
                FollowingInfoFragment.newInstance(username)
        }
    }
}

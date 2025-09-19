package com.example.anikutusu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.anikutusu.adapter.ShowMemoryAdapter
import com.example.anikutusu.databinding.FragmentOtherProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage

class OtherProfileFragment : Fragment() {

    private lateinit var binding: FragmentOtherProfileBinding

    private val db by lazy { FirebaseDatabase.getInstance().reference.child("Users") }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val list = arrayListOf<ShowMemoryDataClass>()
    private lateinit var adapter: ShowMemoryAdapter

    private fun String.sanitizeKey() = this.replace(".", "_")

    // "photo" KELİMESİNDEN SONRAKİ sayıyı al (örn: Umutcanyigitphoto4.jpg -> 4)
    private fun extractIndexFromName(name: String): Int {
        val match = Regex("photo(\\d+)", RegexOption.IGNORE_CASE).find(name)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOtherProfileBinding.inflate(inflater, container, false)

        // 1) Argüman: görüntülenen profilin kullanıcı adı
        val profileUserName = (arguments?.getString("username") ?: "").trim()
        binding.userName.text = profileUserName

        // 2) Key’ler
        val profileKey = "${profileUserName}Data".sanitizeKey()
        val loginUserName = (auth.currentUser?.displayName ?: "").trim()
        val loginKey = "${loginUserName}Data".sanitizeKey()

        // 3) RecyclerView kurulum
        adapter = ShowMemoryAdapter(requireContext(), list)
        binding.rv.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = this@OtherProfileFragment.adapter
        }

        // 4) Profil resmi (opsiyonel örnek)
        val storageRef = com.google.firebase.Firebase.storage.reference
            .child("images/f348f647-eba8-4d8a-a6a3-682fc4622783.jpg")

        storageRef.downloadUrl
            .addOnSuccessListener { uri ->
                if (!isAdded) return@addOnSuccessListener
                Glide.with(this@OtherProfileFragment)
                    .load(uri)
                    .circleCrop()
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(binding.imgProfile)
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                binding.imgProfile.setImageResource(android.R.drawable.ic_menu_report_image)
            }

        // 5) Görüntülenen kullanıcının fotoğraflarını yükle (username’e göre)
        loadUserImages(profileKey)

        // 6) Takip et
        binding.followButton.setOnClickListener {
            if (profileUserName.isEmpty() || loginUserName.isEmpty()) return@setOnClickListener

            db.child(loginKey).child("userFollowingList")
                .child("${loginUserName}Followed${profileUserName}".sanitizeKey())
                .setValue(profileUserName)

            db.child(profileKey).child("userFollowerList")
                .child("${profileUserName}Follower${loginUserName}".sanitizeKey())
                .setValue(loginUserName)
        }

        // 7) SAYILAR (Realtime)
        // Takipçi (followers)
        db.child(profileKey).child("userFollowerList")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    if (!isAdded) return
                    binding.followersnumbers.text = snap.childrenCount.toString()
                }
                override fun onCancelled(err: DatabaseError) {
                    Log.e("followersRef", err.message)
                }
            })

        // Takip edilen (following)
        db.child(profileKey).child("userFollowingList")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    if (!isAdded) return
                    binding.followednumbers.text = snap.childrenCount.toString()
                }
                override fun onCancelled(err: DatabaseError) {
                    Log.e("followingRef", err.message)
                }
            })

        // 8) Rozet butonu (badge ekranına git)
        binding.RozetButton.setOnClickListener {
            findNavController().navigate(R.id.badgeFragment)
        }

        // 9) Followers / Following sayfaları (global action + initialTab)
        fun goFollowInfo(initialTab: String) {
            val bundle = Bundle().apply {
                putString("username", profileUserName)   // Hangi profilin listesi
                putString("initialTab", initialTab)      // "followers" | "following"
                putString("followersCount", binding.followersnumbers.text.toString())
                putString("followingCount", binding.followednumbers.text.toString())
            }
            findNavController().navigate(R.id.action_global_followInfoFragment, bundle)
        }
        binding.followersnumbers.setOnClickListener { goFollowInfo("followers") }
        binding.followednumbers.setOnClickListener { goFollowInfo("following") }

        return binding.root
    }

    /** username’e ait fotoları Firebase Storage -> users/{profileKey}/images yolundan çekip grid’e basar */
    private fun loadUserImages(profileKey: String) {
        val imagesRef: StorageReference = FirebaseStorage.getInstance().reference
            .child("users")
            .child(profileKey)
            .child("images")

        // Temiz başla
        list.clear()
        adapter.notifyDataSetChanged()
        binding.memorynumbers.text = "0"

        imagesRef.listAll()
            .addOnSuccessListener { result ->
                if (!isAdded) return@addOnSuccessListener

                val total = result.items.size
                if (total == 0) {
                    binding.memorynumbers.text = "0"
                    return@addOnSuccessListener
                }

                // "photo"dan SONRAKİ sayıya göre BÜYÜKTEN KÜÇÜĞE sırala (photo12, photo11, ...)
                val sortedItems = result.items.sortedByDescending { item ->
                    extractIndexFromName(item.name)
                }

                val temp = arrayOfNulls<ShowMemoryDataClass>(total)
                var completed = 0

                sortedItems.forEachIndexed { idx, item ->
                    item.downloadUrl
                        .addOnSuccessListener { uri ->
                            if (!isAdded) return@addOnSuccessListener
                            temp[idx] = ShowMemoryDataClass(uri.toString())
                        }
                        .addOnCompleteListener {
                            completed++
                            if (completed == total && isAdded) {
                                list.clear()
                                list.addAll(temp.filterNotNull())
                                adapter.notifyDataSetChanged()
                                binding.memorynumbers.text = list.size.toString()
                            }
                        }
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                binding.memorynumbers.text = "0"
            }
    }
}

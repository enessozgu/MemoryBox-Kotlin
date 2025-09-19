package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.anikutusu.adapter.ShowMemoryAdapter
import com.example.anikutusu.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val usersRef by lazy { FirebaseDatabase.getInstance().reference.child("Users") }

    private var followersListener: ValueEventListener? = null
    private var followingListener: ValueEventListener? = null

    private val list = arrayListOf<ShowMemoryDataClass>()
    private lateinit var adapter: ShowMemoryAdapter

    private fun String.sanitizeKey() = replace(".", "_")

    // "photo" KELİMESİNDEN SONRAKİ sayıyı al (örn: Umutcanyigitphoto4.jpg -> 4)
    private fun extractIndexFromName(name: String): Int {
        val match = Regex("photo(\\d+)", RegexOption.IGNORE_CASE).find(name)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        // RecyclerView
        adapter = ShowMemoryAdapter(requireContext(), list)
        binding.rv.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = this@ProfileFragment.adapter
        }

        // Kullanıcı adı & key
        val displayName = auth.currentUser?.displayName?.trim().orEmpty()
        val email = auth.currentUser?.email.orEmpty()
        val fallbackName = email.substringBefore('@')
        val profileUserName = (if (displayName.isNotEmpty()) displayName else fallbackName).trim()
        val profileKey = "${profileUserName}Data".sanitizeKey()

        binding.userName.text = profileUserName

        // Fotoğrafları yükle (sıralı)
        loadUserImages(profileKey)

        return binding.root
    }

    private fun loadUserImages(profileKey: String) {
        val imagesRef = FirebaseStorage.getInstance().reference
            .child("users")
            .child(profileKey)
            .child("images")

        // Temiz başla
        list.clear()
        adapter.notifyDataSetChanged()
        binding.memorynumbers.text = "0"

        imagesRef.listAll()
            .addOnSuccessListener { result ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val total = result.items.size
                if (total == 0) {
                    binding.memorynumbers.text = "0"
                    return@addOnSuccessListener
                }

                // 1) "photo"dan SONRAKİ sayıya göre BÜYÜKTEN KÜÇÜĞE sırala (photo12, photo11, ...)
                val sortedItems = result.items.sortedByDescending { item ->
                    extractIndexFromName(item.name)
                }

                // 2) Sıralı sonucu korumak için geçici dizi + tamamlama sayacı
                val temp = arrayOfNulls<ShowMemoryDataClass>(total)
                var completed = 0

                // 3) Sıralı indeksle URL’leri çek
                sortedItems.forEachIndexed { idx, item ->
                    item.downloadUrl
                        .addOnSuccessListener { uri ->
                            if (!isAdded || _binding == null) return@addOnSuccessListener
                            temp[idx] = ShowMemoryDataClass(uri.toString())
                        }
                        .addOnCompleteListener {
                            completed++
                            if (completed == total && isAdded && _binding != null) {
                                // 4) Hepsi bitti → tek seferde güncelle
                                list.clear()
                                list.addAll(temp.filterNotNull())
                                adapter.notifyDataSetChanged()
                                binding.memorynumbers.text = list.size.toString()
                            }
                        }
                }
            }
            .addOnFailureListener {
                binding.memorynumbers.text = "0"
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Profil resmi (örnek)
        val storageRef = com.google.firebase.Firebase.storage.reference
            .child("images/f348f647-eba8-4d8a-a6a3-682fc4622783.jpg")

        storageRef.downloadUrl
            .addOnSuccessListener { uri ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(binding.imgProfile)
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.imgProfile.setImageResource(android.R.drawable.ic_menu_report_image)
            }

        // Realtime sayaçlar
        val displayName = auth.currentUser?.displayName?.trim().orEmpty()
        val email = auth.currentUser?.email.orEmpty()
        val fallbackName = email.substringBefore('@')
        val profileUserName = (if (displayName.isNotEmpty()) displayName else fallbackName).trim()
        val profileKey = "${profileUserName}Data".sanitizeKey()

        val followersRef = usersRef.child(profileKey).child("userFollowerList")
        val followingRef = usersRef.child(profileKey).child("userFollowingList")

        followersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return
                binding.followersnumbers.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        followingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return
                binding.followednumbers.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        followersRef.addValueEventListener(followersListener!!)
        followingRef.addValueEventListener(followingListener!!)

        // FollowInfo geçişi
        fun goFollowInfo(initialTab: String) {
            val bundle = Bundle().apply {
                putString("username", profileUserName)
                putString("initialTab", initialTab)
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

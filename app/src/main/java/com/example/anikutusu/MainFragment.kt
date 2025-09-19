package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anikutusu.adapter.ShowMemoryInMainPageAdapter
import com.example.anikutusu.databinding.FragmentMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataList: ArrayList<ShowMemoryMainPageDataClass>
    private lateinit var adapter: ShowMemoryInMainPageAdapter

    // Firebase
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private val currentUid by lazy { auth.currentUser?.uid ?: "anonymous" }

    // 🔐 RTDB key sanitizer (., #, $, [, ] -> _)
    private fun fbSafeKey(input: String): String =
        input.replace(Regex("[.#$\\[\\]]"), "_")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        // RecyclerView
        binding.rv.setHasFixedSize(true)
        binding.rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        dataList = arrayListOf()

        // Adapter (callback’li)
        adapter = ShowMemoryInMainPageAdapter(
            requireContext(),
            dataList,
            onLikeChanged = { item, _, liked ->
                val likesRef = db.child("likes").child(item.id) // item.id güvenli
                if (liked) likesRef.child(currentUid).setValue(true)
                else likesRef.child(currentUid).removeValue()
            },
            onCommentClick = { _, _ ->
                CommentsBottomSheetFragment().show(parentFragmentManager, "comments")
            }
        )
        binding.rv.adapter = adapter

        // Storage’tan görselleri çek
        val imagesRef = FirebaseStorage.getInstance().reference
            .child("users")
            .child("Enes ÖzgüData")
            .child("images")

        imagesRef.listAll()
            .addOnSuccessListener { result ->
                result.items.forEach { item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
                        // ✅ Güvenli ve benzersiz postId üretimi:
                        // path'i baz al, / -> _, yasak karakterleri temizle, uzantıyı at.
                        val raw = item.path.replace("/", "_")
                        val base = fbSafeKey(raw)
                        val withoutExt = base.substringBeforeLast('.') // .jpg, .png vs. at
                        // Ek benzersizlik: generation eklemek istersen aşağıyı kullan:
                        // val postId = "${withoutExt}_${item.generation}"
                        val postId = withoutExt

                        val model = ShowMemoryMainPageDataClass(
                            id = postId,
                            userName = "Enes",      // TODO: dinamikleştirilebilir
                            location = "Ankara",    // TODO: dinamikleştirilebilir
                            imageUrl = uri.toString(),
                            isLiked = false
                        )
                        dataList.add(model)
                        adapter.notifyItemInserted(dataList.lastIndex)

                        // (Opsiyonel) Kullanıcı daha önce beğenmiş mi?
                        // db.child("likes").child(postId).child(currentUid).get().addOnSuccessListener { snap ->
                        //     if (snap.exists()) {
                        //         model.isLiked = true
                        //         val idx = dataList.indexOfFirst { it.id == postId }
                        //         if (idx != -1) adapter.notifyItemChanged(idx, "payload_like_changed")
                        //     }
                        // }
                    }
                }
            }
            .addOnFailureListener { it.printStackTrace() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anikutusu.adapter.ShowMemoryInMainPageAdapter
import com.example.anikutusu.databinding.FragmentMainBinding
import com.google.firebase.storage.FirebaseStorage

class MainFragment : Fragment() {

    // 🔒 ViewBinding (leaksiz)
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    // 🔧 Liste + Adapter
    private lateinit var dataList: ArrayList<ShowMemoryMainPageDataClass>
    private lateinit var adapter: ShowMemoryInMainPageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        // RecyclerView
        binding.rv.setHasFixedSize(true)
        binding.rv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        // Adapter
        dataList = ArrayList()
        adapter = ShowMemoryInMainPageAdapter(requireContext(), dataList)
        binding.rv.adapter = adapter

        // Firebase Storage referansı
        val imagesRef = FirebaseStorage.getInstance().reference
            .child("users")
            .child("UmutcanyigitData")
            .child("images")

        // Klasördeki tüm görselleri çek ve listeye ekle
        imagesRef.listAll()
            .addOnSuccessListener { result ->
                result.items.forEach { item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
                        dataList.add(
                            ShowMemoryMainPageDataClass(
                                userName = "Enes",   // İstersen dinamik yap
                                location = "Ankara", // İstersen dinamik yap
                                imageUrl = uri.toString()
                            )
                        )
                        adapter.notifyItemInserted(dataList.lastIndex)
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                // İsteğe bağlı: Toast/Log ekleyebilirsin
            }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

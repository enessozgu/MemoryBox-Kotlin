package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anikutusu.adapter.MemoryAdapter
import com.example.anikutusu.databinding.FragmentMemoryListBinding
import com.example.anikutusu.model.MemoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MemoryListFragment : Fragment() {

    private var _binding: FragmentMemoryListBinding? = null
    private val binding get() = _binding!!
    private val adapter = MemoryAdapter { memoryItem ->
        // 👇 Tıklanınca yapılacak işlemler burada!
        Toast.makeText(requireContext(), "Anı: ${memoryItem.text}", Toast.LENGTH_SHORT).show()
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewMemories.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewMemories.adapter = adapter

        loadMemories()
    }

    private fun loadMemories() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("memories")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val list = result.map { doc ->
                    MemoryItem(
                        text = doc.getString("text") ?: "",
                        photoUrl = doc.getString("photoUrl"),
                        audioUrl = doc.getString("audioUrl")
                    )
                }
                adapter.updateList(list)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Anılar yüklenemedi", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

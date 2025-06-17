package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anikutusu.adapter.MemoryAdapter
import com.example.anikutusu.databinding.FragmentMemoryListBinding
import com.example.anikutusu.model.MemoryItem
import com.google.android.gms.maps.GoogleMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MemoryListFragment : Fragment() {

    private lateinit var googleMap: GoogleMap



    private var _binding: FragmentMemoryListBinding? = null
    private val binding get() = _binding!!

    private val adapter = MemoryAdapter { memoryItem ->
        val fragment = MemoryDetailBottomSheetFragment()
        fragment.arguments = Bundle().apply {
            putString("text", memoryItem.text)
            putString("photoUrl", memoryItem.photoUrl)
            putString("audioUrl", memoryItem.audioUrl)
            putDouble("latitude", memoryItem.latitude)
            putDouble("longitude", memoryItem.longitude)
            putLong("timestamp", memoryItem.timestamp ?: 0L)
            putString("userId", memoryItem.userId)
        }
        fragment.show(parentFragmentManager, fragment.tag)
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


        binding.homeNavigationView.setNavigationItemSelectedListener {
            showDrawerMenuItemAction(it.itemId)
            true
        }




        val toolbar = binding.toolbar
        toolbar.title = "Anılarım"

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(toolbar)

        val drawerLayout = binding.drawerLayout

        val toggle = ActionBarDrawerToggle(
            activity,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()








    }







    private fun showDrawerMenuItemAction(menuItemId: Int) {
        when (menuItemId) {

            R.id.nav_map -> {

                findNavController().navigate(R.id.action_memoryListFragment_to_homeMapFragment)
            }


            R.id.nav_badges -> {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(requireContext(), "Çıkış yapıldı", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_memoryListFragment_to_registerFragment)
            }

            R.id.nav_memories -> {
                Toast.makeText(requireContext(), "Zaten buradasın 👀", Toast.LENGTH_SHORT).show()
            }

            R.id.nav_settings -> {

            }
        }
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
                        audioUrl = doc.getString("audioUrl"),
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        userId = doc.getString("userId") ?: ""
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
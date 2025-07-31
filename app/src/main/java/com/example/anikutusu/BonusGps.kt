package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anikutusu.BonusGpsDirections
import com.example.anikutusu.adapter.BonusAdapter


import com.example.anikutusu.databinding.FragmentBonusgpsBinding
import com.example.anikutusu.model.LocationModel
import com.google.android.gms.maps.GoogleMap
import com.google.firebase.auth.FirebaseAuth


class BonusGps : Fragment() {

    private lateinit var binding: FragmentBonusgpsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBonusgpsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigation Drawer ayarları
        val toolbar = binding.toolbar
        toolbar.title = "Memory Box"
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

        binding.homeNavigationView.setNavigationItemSelectedListener {
            showDrawerMenuItemAction(it.itemId)
            true
        }

        // Lokasyon listesi
        val locationList = listOf(
            LocationModel("Kız Kulesi", "İstanbul’un simgelerinden.", 41.0211, 29.0049),
            LocationModel("Galata Kulesi", "Tarihi kule.", 41.0256, 28.9744),
            LocationModel("Taksim Meydanı", "İstanbul’un kalbi.", 41.0369, 28.9850),
            LocationModel("Moda Sahili", "Kadıköy'ün keyfi.", 40.9824, 29.0290)
        )

        val adapter = BonusAdapter(
            locationList,
            onShowOnMapClicked = { location ->
                val action = BonusGpsDirections.actionBonusgpsToHomeMapFragment(
                    location.latitude.toFloat(),
                    location.longitude.toFloat()
                )
                findNavController().navigate(action)
            },
            onAddMemoryClicked = { location ->
                val action = BonusGpsDirections.actionBonusgpsToMemoryListFragment(
                    location.latitude.toFloat(),
                    location.longitude.toFloat()
                )
                findNavController().navigate(action)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }


    private fun showDrawerMenuItemAction(menuItemId: Int) {
        when (menuItemId) {
            R.id.nav_map -> findNavController().navigate(R.id.action_bonusgps_to_homeMapFragment)
            R.id.nav_badges -> {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(requireContext(), "Exit made", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_homeMapFragment_to_registerFragment)
            }
            R.id.nav_memories -> findNavController().navigate(R.id.action_bonusgps_to_memoryListFragment)
            R.id.nav_rozet -> Toast.makeText(requireContext(), "You're already here 👀", Toast.LENGTH_SHORT).show()
            R.id.nav_settings -> { /* Ayarlar ekranı */ }
        }
    }





    }






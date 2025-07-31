package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anikutusu.BonusGpsDirections


import com.example.anikutusu.databinding.FragmentBonusgpsBinding



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
        val locationList = listOf(
            LocationModel("Kız Kulesi", "İstanbul’un simgelerinden.", 41.0211, 29.0049),
            LocationModel("Galata Kulesi", "Tarihi kule.", 41.0256, 28.9744),
            LocationModel("Taksim Meydanı", "İstanbul’un kalbi.", 41.0369, 28.9850),
            LocationModel("Moda Sahili", "Kadıköy'ün keyfi.", 40.9824, 29.0290)
        )

        val adapter = BonusAdapter(locationList,
            onShowOnMapClicked = { location ->
                // Navigate to HomeMapFragment with location data
                val action = BonusGpsDirections.actionBonusgpsToHomeMapFragment(
                    location.latitude.toFloat(),
                    location.longitude.toFloat()
                )
                findNavController().navigate(action)

            },
            onAddMemoryClicked = { location ->
                // Navigate to MemoryAddMode with location data
                val action = BonusGpsDirections.actionBonusgpsToMemoryListFragment(
                    location.latitude.toFloat(),
                    location.longitude.toFloat()
                )
                findNavController().navigate(action)

            }
        )

        // RecyclerView bağlama
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }
}


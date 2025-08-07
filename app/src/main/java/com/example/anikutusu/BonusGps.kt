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

// Fragment that shows a location-based menu using a navigation drawer and RecyclerView
class BonusGps : Fragment() {

    private lateinit var binding: FragmentBonusgpsBinding // ViewBinding for accessing layout views

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout and initialize binding
        binding = FragmentBonusgpsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Set up the Navigation Drawer ---
        val toolbar = binding.toolbar
        toolbar.title = "Memory Box"

        // Setup toolbar with parent activity
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(toolbar)

        // Configure drawer toggle (hamburger icon behavior)
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

        // Handle navigation item selection
        binding.homeNavigationView.setNavigationItemSelectedListener {
            showDrawerMenuItemAction(it.itemId)
            true
        }

        // --- Location List ---
        // Predefined list of iconic locations in Istanbul
        val locationList = listOf(
            LocationModel("Kız Kulesi", "İstanbul’un simgelerinden.", 41.0211, 29.0049),
            LocationModel("Galata Kulesi", "Tarihi kule.", 41.0256, 28.9744),
            LocationModel("Taksim Meydanı", "İstanbul’un kalbi.", 41.0369, 28.9850),
            LocationModel("Moda Sahili", "Kadıköy'ün keyfi.", 40.9824, 29.0290)
        )

        // Adapter for displaying the location list in RecyclerView
        val adapter = BonusAdapter(
            locationList,
            onShowOnMapClicked = { location ->
                // Navigate to map fragment with selected coordinates
                val action = BonusGpsDirections.actionBonusgpsToHomeMapFragment(
                    location.latitude.toFloat(),
                    location.longitude.toFloat()
                )
                findNavController().navigate(action)
            },
            onAddMemoryClicked = { location ->
                // Navigate to memory list with selected coordinates
                val action = BonusGpsDirections.actionBonusgpsToMemoryListFragment(
                    location.latitude.toFloat(),
                    location.longitude.toFloat()
                )
                findNavController().navigate(action)
            }
        )

        // Set up RecyclerView with layout manager and adapter
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }

    // Handles what happens when navigation drawer items are clicked
    private fun showDrawerMenuItemAction(menuItemId: Int) {
        when (menuItemId) {
            R.id.nav_map -> findNavController().navigate(R.id.action_bonusgps_to_homeMapFragment)
            R.id.nav_badges -> {
                // Sign out and navigate to register screen
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(requireContext(), "Exit made", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_homeMapFragment_to_registerFragment)
            }
            R.id.nav_memories -> findNavController().navigate(R.id.action_bonusgps_to_memoryListFragment)
            R.id.nav_rozet -> Toast.makeText(requireContext(), "You're already here 👀", Toast.LENGTH_SHORT).show()
            R.id.nav_settings -> { /* Settings screen can be handled here */ }
        }
    }

}

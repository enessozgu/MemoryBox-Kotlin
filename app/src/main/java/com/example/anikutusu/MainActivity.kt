package com.example.anikutusu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.anikutusu.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

// The main entry point of the application
// It checks the user's authentication state and navigates to the appropriate screen
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // ViewBinding for main activity layout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get an instance of FirebaseAuth to check login status
      val auth = FirebaseAuth.getInstance()

        // Retrieve NavController from the NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val currentDestination = navController.currentDestination?.id

        if (auth.currentUser != null) {
            // If user is already logged in, navigate to HomeMapFragment
            if (currentDestination != R.id.homePageFragment) {
                navController.navigate(R.id.homePageFragment)
            }
        } else {
            // If user is not logged in, navigate to RegisterFragment
            if (currentDestination != R.id.registerFragment) {
                navController.navigate(R.id.registerFragment)
            }
        }
    }
}

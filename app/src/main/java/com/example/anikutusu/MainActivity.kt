package com.example.anikutusu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.anikutusu.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val auth = FirebaseAuth.getInstance()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val currentDestination = navController.currentDestination?.id

        if (auth.currentUser != null) {
            // Giriş yapılmışsa, HomeMapFragment
            if (currentDestination != R.id.homeMapFragment) {
                navController.navigate(R.id.homeMapFragment)
            }
        } else {
            // Giriş yapılmamışsa, RegisterFragment (kayıt)
            if (currentDestination != R.id.registerFragment) {
                navController.navigate(R.id.registerFragment)
            }
        }
    }
}

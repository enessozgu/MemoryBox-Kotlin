package com.example.anikutusu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController

import com.google.firebase.auth.FirebaseAuth

import com.example.anikutusu.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider

// Fragment responsible for handling user login with email/password and Google Sign-In
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding // View binding for accessing UI elements
    private lateinit var auth: FirebaseAuth             // Firebase authentication instance
    private lateinit var googleSignInClient: GoogleSignInClient // Google Sign-In client

    // Called when the view is being created
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root

        // ⚠️ This line will never be reached because of the return above
        auth = FirebaseAuth.getInstance()
    }

    // Called when the view is ready and attached to the fragment
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // --- Configure Google Sign-In ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Token for Firebase credential
            .requestEmail() // Request email address
            .build()

        // Initialize GoogleSignInClient with the configured options
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // When the user clicks the Google sign-in button
        binding.googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent) // Start Google Sign-In flow
        }

        // When the user clicks the email/password login button
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Attempt to sign in using Firebase email/password
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                            // Navigate to the map screen
                            findNavController().navigate(R.id.action_loginFragment_to_homeMapFragment)
                        } else {
                            Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                // Show error if either field is empty
                Toast.makeText(requireContext(), "Email and password cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle the result of Google Sign-In flow
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            // Get account and create Firebase credential
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            // Sign in with Firebase using the Google credential
            auth.signInWithCredential(credential)
                .addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        Toast.makeText(requireContext(), "Signed in with Google!", Toast.LENGTH_SHORT).show()
                        // Navigate to the map screen
                        findNavController().navigate(R.id.action_loginFragment_to_homeMapFragment)
                    } else {
                        Toast.makeText(requireContext(), "Google sign-in failed: ${signInTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            // Google Sign-In was cancelled or failed
            Toast.makeText(requireContext(), "Google sign-in was cancelled", Toast.LENGTH_SHORT).show()
        }
    }
}

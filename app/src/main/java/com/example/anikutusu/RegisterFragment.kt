package com.example.anikutusu

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.example.anikutusu.databinding.FragmentRegisterBinding

// Fragment that handles user registration using Email/Password and Google Sign-In
class RegisterFragment : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient // Google Sign-In client
    private lateinit var binding: FragmentRegisterBinding // ViewBinding for UI elements
    private lateinit var auth: FirebaseAuth // Firebase authentication

    // Inflate the layout using ViewBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Called when the view is ready
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Required for Firebase auth
            .requestEmail()
            .build()

        // Build the GoogleSignInClient with the options
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // If user clicks "Go to Login", navigate to LoginFragment
        binding.textViewGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // Handle Google Sign-In button click
        binding.googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent) // Start Google Sign-In flow
        }

        // Handle email/password registration
        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            // Check if email is not blank and password is strong enough
            if (email.isNotBlank() && password.length >= 6) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()
                            // Navigate to map fragment after successful registration
                            findNavController().navigate(R.id.action_registerFragment_to_homeMapFragment)
                        } else {
                            Toast.makeText(requireContext(), "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(requireContext(), "Enter a valid email and password (6+ chars)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handles the result of the Google Sign-In flow
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            // Sign in to Firebase using the Google credential
            auth.signInWithCredential(credential)
                .addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        Toast.makeText(requireContext(), "Signed in with Google!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registerFragment_to_homeMapFragment)
                    } else {
                        Toast.makeText(requireContext(), "Google sign-in failed: ${signInTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(requireContext(), "Google sign-in cancelled", Toast.LENGTH_SHORT).show()
        }
    }
}

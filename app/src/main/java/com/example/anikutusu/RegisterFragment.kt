package com.example.anikutusu

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.example.anikutusu.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.FirebaseDatabase

// Fragment responsible for handling user registration via Email/Password and Google Sign-In
class RegisterFragment : Fragment() {

    // Google Sign-In client instance
    private lateinit var googleSignInClient: GoogleSignInClient
    // View binding for accessing layout elements
    private lateinit var binding: FragmentRegisterBinding
    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout with ViewBinding
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In options (ID token + email)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        // Create Google Sign-In client
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Navigate to Login screen
        binding.textViewGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // Handle Google Sign-In button click
        binding.googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // Handle Email/Password registration button click
        binding.signUp.setOnClickListener {
            val email = binding.mailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            // Validate email and password (minimum 6 characters for password)
            if (email.isNotBlank() && password.length >= 6) {
                // Attempt to create a new user with Firebase
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            // Send email verification to the new user
                            user?.sendEmailVerification()
                                ?.addOnCompleteListener { emailTask ->
                                    if (emailTask.isSuccessful) {
                                        // Success: Notify user and redirect to Login
                                        Snackbar.make(
                                            requireActivity().findViewById(android.R.id.content),
                                            "Registration successful! Please verify your email.",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                        val database=FirebaseDatabase.getInstance()
                                        val userList=database.getReference("Users")
                                        val user=UserDataFormat(binding.usernameEditText.text.toString(),binding.mailEditText.text.toString(),0)
                                        userList.child("${binding.usernameEditText.text}"+"Data").setValue(user)
                                        auth.signOut()
                                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                                    } else {
                                        // Failed to send verification email
                                        Snackbar.make(
                                            requireActivity().findViewById(android.R.id.content),
                                            "Failed to send verification email: ${emailTask.exception?.message}",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        } else {
                            // Registration failed (e.g., email already in use)
                            Snackbar.make(
                                requireActivity().findViewById(android.R.id.content),
                                "Registration failed: ${task.exception?.message}",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                // Invalid input: Show error
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    "Please enter a valid email and password (6+ characters).",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Handle result of Google Sign-In process
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                // Authenticate with Firebase using Google credentials
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            Snackbar.make(
                                requireActivity().findViewById(android.R.id.content),
                                "Signed in with Google!",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            findNavController().navigate(R.id.action_registerFragment_to_homeMapFragment)
                        } else {
                            Snackbar.make(
                                requireActivity().findViewById(android.R.id.content),
                                "Google sign-in failed: ${signInTask.exception?.message}",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                // User cancelled Google Sign-In
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    "Google sign-in was cancelled",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
}

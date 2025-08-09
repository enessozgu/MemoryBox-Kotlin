package com.example.anikutusu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.anikutusu.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider

// Fragment responsible for handling user login via Email/Password and Google Sign-In
class LoginFragment : Fragment() {

    // View binding for accessing UI elements
    private lateinit var binding: FragmentLoginBinding
    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth
    // Google Sign-In client
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout using ViewBinding
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In options (ID token + email request)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Create Google Sign-In client with above configuration
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Handle Google Sign-In button click
        binding.googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent) // Launch Google Sign-In flow
        }

        // Navigate to Forgot Password screen
        binding.forgotpassword.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_loginFragment_to_forgotPassword)
        }

        // Navigate to Registration screen
        binding.textViewGoToRegister.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Handle Email/Password login button click
        binding.signin.setOnClickListener {
            val email = binding.mailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            // Validate input fields
            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(binding.root, "Email and password cannot be empty!", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Disable sign-in button during authentication process
            binding.signin.isEnabled = false

            // Attempt Firebase Email/Password authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    binding.signin.isEnabled = true // Re-enable button
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        // Check if email is verified before allowing login
                        if (user != null && user.isEmailVerified) {
                            Snackbar.make(binding.root, "Login successful!", Snackbar.LENGTH_LONG).show()
                            findNavController().navigate(R.id.action_loginFragment_to_homeMapFragment)
                        } else {
                            Snackbar.make(binding.root, "Please verify your email before logging in.", Snackbar.LENGTH_LONG).show()
                            auth.signOut()
                        }
                    } else {
                        // Display error message from Firebase
                        Snackbar.make(
                            binding.root,
                            "Login failed: ${task.exception?.message ?: "Unknown error"}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    // Handle the result of Google Sign-In flow
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            // Authenticate with Firebase using Google credentials
            auth.signInWithCredential(credential)
                .addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        Snackbar.make(binding.root, "Signed in with Google!", Snackbar.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_loginFragment_to_homeMapFragment)
                    } else {
                        Snackbar.make(
                            binding.root,
                            "Google sign-in failed: ${signInTask.exception?.message ?: "Unknown error"}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
        } else {
            // User cancelled Google Sign-In
            Snackbar.make(binding.root, "Google sign-in was cancelled", Snackbar.LENGTH_LONG).show()
        }
    }
}

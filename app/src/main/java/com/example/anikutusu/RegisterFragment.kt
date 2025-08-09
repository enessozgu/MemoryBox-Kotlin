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

class RegisterFragment : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: FragmentRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.textViewGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.signUp.setOnClickListener {
            val email = binding.mailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (email.isNotBlank() && password.length >= 6) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            user?.sendEmailVerification()
                                ?.addOnCompleteListener { emailTask ->
                                    if (emailTask.isSuccessful) {
                                        Snackbar.make(
                                            requireActivity().findViewById(android.R.id.content),
                                            "Registration successful! Please verify your email.",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                        auth.signOut()
                                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                                    } else {
                                        Snackbar.make(
                                            requireActivity().findViewById(android.R.id.content),
                                            "Failed to send verification email: ${emailTask.exception?.message}",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        } else {
                            Snackbar.make(
                                requireActivity().findViewById(android.R.id.content),
                                "Registration failed: ${task.exception?.message}",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    "Please enter a valid email and password (6+ characters).",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
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
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    "Google sign-in was cancelled",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
}

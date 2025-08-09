package com.example.anikutusu

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.anikutusu.databinding.FragmentForgotPasswordBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

// Fragment responsible for handling password reset via email
class ForgotPassword : Fragment() {

    // View binding for accessing UI elements
    private lateinit var binding: FragmentForgotPasswordBinding
    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate layout with ViewBinding
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Handle "Send Reset Link" button click
        binding.sendresetlink.setOnClickListener {
            val email = binding.mailEditText.text.toString().trim()

            // Validate that the email field is not empty
            if (email.isEmpty()) {
                Snackbar.make(binding.root, "Please enter your email", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            // Validate that the entered email matches email format
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Snackbar.make(binding.root, "Please enter a valid email address", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Disable button to prevent multiple clicks
            binding.sendresetlink.isEnabled = false

            // Send password reset email via Firebase
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    // Re-enable button after task completes
                    binding.sendresetlink.isEnabled = true
                    if (task.isSuccessful) {
                        // Generic message to prevent email enumeration
                        Snackbar.make(
                            binding.root,
                            "If an account exists for this email, a reset link has been sent.",
                            Snackbar.LENGTH_LONG
                        ).show()
                        // Return to previous screen (Login)
                        findNavController().popBackStack()
                    } else {
                        Snackbar.make(binding.root, "Failed to send reset link", Snackbar.LENGTH_LONG).show()
                    }
                }
        }

        // Handle "Create a New Account" link click
        binding.createAccount.setOnClickListener {
            Navigation.findNavController(it)
                .navigate(R.id.action_forgotPassword_to_registerFragment)
        }
    }
}

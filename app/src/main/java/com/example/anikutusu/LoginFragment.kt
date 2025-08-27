package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.anikutusu.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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

        // Google ile giriş
        binding.googleSignInButton.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }


        binding.forgotpassword.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_loginFragment_to_forgotPassword)
        }


        binding.textViewGoToRegister.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_loginFragment_to_registerFragment)
        }


        binding.signin.setOnClickListener {
            val email = binding.mailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                snack("Email and password cannot be empty!")
                return@setOnClickListener
            }

            binding.signin.isEnabled = false
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    binding.signin.isEnabled = true

                    if (!task.isSuccessful) {
                        snack("Login failed: ${task.exception?.message ?: "Unknown error"}")
                        return@addOnCompleteListener
                    }

                    val user = auth.currentUser
                    if (user == null) {
                        snack("Login failed: user is null")
                        return@addOnCompleteListener
                    }

                    if (!user.isEmailVerified) {
                        snack("Please verify your email before logging in.")
                        auth.signOut()
                        return@addOnCompleteListener
                    }


                    setMailApprovalByEmail(email) {
                        snack("Login successful!")
                        var vt=UserDatabase(requireContext())
                        UserDatabaseFunc().updateData(vt,binding.mailEditText.toString())
                        findNavController().navigate(R.id.action_loginFragment_to_homePageFragment)
                    }
                }
        }
    }


    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (!task.isSuccessful) {
                snack("Google sign-in was cancelled")
                return@registerForActivityResult
            }

            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            auth.signInWithCredential(credential)
                .addOnCompleteListener { signInTask ->
                    if (!signInTask.isSuccessful) {
                        snack("Google sign-in failed: ${signInTask.exception?.message ?: "Unknown error"}")
                        return@addOnCompleteListener
                    }

                    // Google hesabının maili (önce auth, yoksa account'tan)
                    val email = auth.currentUser?.email ?: account.email
                    if (email.isNullOrBlank()) {
                        snack("Signed in with Google!")
                        findNavController().navigate(R.id.action_loginFragment_to_homePageFragment)
                        return@addOnCompleteListener
                    }

                    setMailApprovalByEmail(email) {
                        snack("Signed in with Google!")
                        findNavController().navigate(R.id.action_loginFragment_to_homePageFragment)
                    }
                }
        }


    private fun setMailApprovalByEmail(email: String, onDone: () -> Unit = {}) {
        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        val update = mapOf<String, Any>("userMailApproval" to 1)

        usersRef.orderByChild("userMail").equalTo(email).limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    val node = ds.children.firstOrNull()
                    if (node != null) {
                        node.ref.updateChildren(update)
                            .addOnCompleteListener { onDone() }
                            .addOnFailureListener { onDone() }
                    } else {
                        onDone()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    onDone()
                }
            })

    }

    private fun snack(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

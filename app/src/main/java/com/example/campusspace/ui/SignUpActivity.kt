package com.example.campusspace.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.campusspace.R
import androidx.appcompat.app.AppCompatActivity
import com.example.campusspace.databinding.SignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: SignupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Set up the click listener for the "LOGIN" text
        binding.loginTextView.setOnClickListener {
            // Create an intent to navigate to the LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            // Optional: finish() if you want to close the signup screen when going to login
            finish()
        }


        binding.signUpButton.setOnClickListener {
            val fullName = binding.fullNameEditText.text.toString().trim()
            val phone = binding.phoneEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = firebaseAuth.currentUser?.uid
                        if (userId != null) {
                            val user = hashMapOf(
                                "fullName" to fullName,
                                "phone" to phone,
                                "email" to email
                            )
                            firestore.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registration successful! Logging in...", Toast.LENGTH_LONG).show()
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }else {
                        val exception = task.exception
                        if (exception is FirebaseAuthException) {
                            when (exception.errorCode) {
                                "ERROR_INVALID_EMAIL" -> {
                                    Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                                }
                                "ERROR_EMAIL_ALREADY_IN_USE" -> {
                                    Toast.makeText(this, "This email address is already in use.", Toast.LENGTH_SHORT).show()
                                }
                                "ERROR_WEAK_PASSWORD" -> {
                                    Toast.makeText(this, "Password is too weak. Please use at least 6 characters.", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    // Handle other Firebase errors
                                    Toast.makeText(this, "Registration failed: ${exception.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    } }

    override fun onBackPressed() {
        super.finish()
        // Apply the reverse transition when the back button is pressed
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

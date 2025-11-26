package com.example.campusspace.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.campusspace.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class ProfileActivity: AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase instances
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Set up the back button on the toolbar
        binding.profileToolbar.setNavigationOnClickListener {
            finish() // Closes this activity and returns to the previous one
        }

        // Load the user's data from Firestore
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            Log.e("ProfileActivity", "User is not logged in.")
            finish() // Close activity if there's no user
            return
        }

        val userRef = firestore.collection("users").document(userId)
        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get data from the document
                    val name = document.getString("fullName") ?: "N/A"
                    val email = document.getString("email") ?: "N/A"
                    val phone = document.getString("phone") ?: "N/A"

                    // Set the text in the UI
                    binding.tvProfileName.text = name
                    binding.tvProfileEmail.text = email
                    binding.tvProfilePhone.text = phone
                } else {
                    Log.d("ProfileActivity", "No profile information found for this user.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileActivity", "Failed to load profile data", exception)
            }
    }
}
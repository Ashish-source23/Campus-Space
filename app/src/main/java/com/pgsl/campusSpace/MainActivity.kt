package com.pgsl.campusSpace

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pgsl.campusSpace.utils.FirebaseAuthUtil
import com.pgsl.campusSpace.utils.FirebaseDB

class MainActivity : AppCompatActivity() {
    private val auth = FirebaseAuthUtil.instance
    private val db = FirebaseDB.instance

    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        checkUserStatus()
    }

    private fun checkUserStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            fetchUserData(currentUser.uid)
        } else {
            Log.d("MainActivity", "No user is signed in.")
        }
    }

    private fun fetchUserData(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val email = document.getString("email") ?: "No email"
                } else {
                    Log.d("MainActivity", "No such user document")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error getting user data", exception)
            }
    }
}

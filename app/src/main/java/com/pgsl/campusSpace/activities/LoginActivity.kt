package com.pgsl.campusSpace.activities

import androidx.appcompat.app.AppCompatActivity
import com.pgsl.campusSpace.utils.FirebaseAuthUtil

class LoginActivity : AppCompatActivity() {
    private val auth = FirebaseAuthUtil.instance

    private fun signInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    val user = auth.currentUser
                } else {
                    // Sign in fails
                }
            }
    }
}

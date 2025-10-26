package com.example.campusspace.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.campusspace.MainActivity
import com.example.campusspace.databinding.LoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signInButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            // First, check if the email has the correct format.
            if (!isValidEmail(email)) {
                binding.emailEditText.error = "Invalid email format. Expected: name+number@iitrpr.ac.in"
                return@setOnClickListener // Stop further processing
            } else {
                binding.emailEditText.error = null // Clear error if format is now correct
            }

            // For this example, we'll use a hardcoded email and password.
            val correctEmail = "2025aim1017@iitrpr.ac.in"
            val correctPassword = "password123"

            if (email == correctEmail && password == correctPassword) {
                // Successful login
                Snackbar.make(binding.root, "Login successful!", Snackbar.LENGTH_SHORT).show()

                // Create an Intent to open MainActivity (the dashboard)
                val intent = Intent(this, MainActivity::class.java)
                // Start the new activity
                startActivity(intent)
                // Close the login screen so the user can't go back to it
                finish()

            } else {
                // Failed login
                Snackbar.make(binding.root, "Invalid email or password", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        // This regex now allows the email to contain letters, numbers, and '+' before the @ symbol.
        val emailRegex = """^[a-zA-Z0-9+]+@iitrpr\.ac\.in$""".toRegex()
        return email.matches(emailRegex)
    }
}

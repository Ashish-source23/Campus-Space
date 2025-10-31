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
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val correctEmail = "2025aim1017@iitrpr.ac.in"
            val correctPassword = "password123"
            if (email.equals(correctEmail, ignoreCase = true) && password == correctPassword) {

                Snackbar.make(binding.root, "Login successful!", Snackbar.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Snackbar.make(binding.root, "Login failed: Invalid email or password", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}

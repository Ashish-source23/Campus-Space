package com.pgsl.campusSpace.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.pgsl.campusSpace.R
import com.pgsl.campusSpace.entity.User
import com.pgsl.campusSpace.services.FirebaseService
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonsLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser != null) {
            navigateToMainActivity()
            return
        }
        setContentView(R.layout.activity_login)
        initializeUI()
        setupClickListeners()
    }

    private fun initializeUI() {
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        registerButton = findViewById(R.id.buttonRegister)
        progressBar = findViewById(R.id.progressBar)
        buttonsLayout = findViewById(R.id.buttonsLayout)
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener { handleAuthAction(isRegister = false) }
        registerButton.setOnClickListener { handleAuthAction(isRegister = true) }
    }

    private fun handleAuthAction(isRegister: Boolean) {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                if (isRegister) {
                    val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                    authResult.user?.let { firebaseUser ->
                        val newUser = User(firebaseUser.uid, firebaseUser.email, firebaseUser.email?.substringBefore('@'))
                        FirebaseService.createUserProfile(newUser) // Log user data in Firestore
                    }
                } else {
                    auth.signInWithEmailAndPassword(email, password).await()
                }
                onAuthSuccess()
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Toast.makeText(this@LoginActivity, "Login Failed: Invalid credentials.", Toast.LENGTH_SHORT).show()
            } catch (e: FirebaseAuthUserCollisionException) {
                Toast.makeText(this@LoginActivity, "Registration Failed: Email already in use.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun onAuthSuccess() {
        FirebaseService.connectPresence() // Log user presence in Realtime Database
        navigateToMainActivity()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        buttonsLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
}

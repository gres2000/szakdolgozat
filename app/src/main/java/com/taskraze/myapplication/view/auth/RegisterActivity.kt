package com.taskraze.myapplication.view.auth

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.taskraze.myapplication.R
import com.taskraze.myapplication.databinding.RegisterActivityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: RegisterActivityBinding
    private lateinit var registerButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var usernameEditText: EditText
    private val firestoreDB = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerButton = findViewById(R.id.buttonRegister)
        emailEditText = findViewById(R.id.editTextEmail)
        emailEditText.setText(intent.getStringExtra("Email"))
        passwordEditText = findViewById(R.id.editTextPassword)
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword)
        usernameEditText = findViewById(R.id.editTextUserName)

        auth = Firebase.auth

        registerButton.setOnClickListener {
            clearErrors()

            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()

            // Validate empty fields
            var hasError = false
            if (username.isEmpty()) { usernameEditText.error = "Field cannot be empty"; hasError = true }
            if (email.isEmpty()) { emailEditText.error = "Field cannot be empty"; hasError = true }
            if (password.isEmpty()) { passwordEditText.error = "Field cannot be empty"; hasError = true }
            if (confirmPassword.isEmpty()) { confirmPasswordEditText.error = "Field cannot be empty"; hasError = true }
            if (hasError) return@setOnClickListener

            // Validate password match
            if (password != confirmPassword) {
                confirmPasswordEditText.error = "Passwords don't match"
                return@setOnClickListener
            }

            // Create user in Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Save additional info in Firestore
                        val userDoc = hashMapOf(
                            "email" to email,
                            "username" to username
                        )
                        firestoreDB.collection("registered_users").document(email)
                            .set(userDoc)
                            .addOnSuccessListener { Log.d(TAG, "User document written") }
                            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        handleFirebaseAuthError(task.exception?.message)
                    }
                }
        }
    }

    private fun clearErrors() {
        usernameEditText.error = null
        emailEditText.error = null
        passwordEditText.error = null
        confirmPasswordEditText.error = null
    }

    private fun handleFirebaseAuthError(message: String?) {
        when {
            message?.contains("email") == true -> emailEditText.error = "Invalid email format"
            message?.contains("weak-password") == true -> passwordEditText.error = "Password too weak"
            else -> Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
        }
    }
}

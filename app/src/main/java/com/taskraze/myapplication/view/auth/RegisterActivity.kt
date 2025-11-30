package com.taskraze.myapplication.view.auth

import AuthViewModelFactory
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.taskraze.myapplication.R
import com.taskraze.myapplication.databinding.RegisterActivityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: RegisterActivityBinding
    private lateinit var registerButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var authViewModel: AuthViewModel

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

        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(this)
        )[AuthViewModel::class.java]

        registerButton.setOnClickListener {
            clearErrors()

            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()

            if (password != confirmPassword) {
                confirmPasswordEditText.error = "Passwords don't match"
                return@setOnClickListener
            }

            authViewModel.register(email, password, username) { success, errorMsg ->
                if (success) {
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    handleFirebaseAuthError(errorMsg)
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

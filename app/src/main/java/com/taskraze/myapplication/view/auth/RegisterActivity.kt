package com.taskraze.myapplication.view.auth

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.taskraze.myapplication.R
import com.taskraze.myapplication.databinding.RegisterActivityBinding
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity: AppCompatActivity() {
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
            val emailInput = emailEditText.text.toString().trim()
            val passwordInput = passwordEditText.text.toString().trim()
            val confirmPasswordInput = confirmPasswordEditText.text.toString().trim()
            val usernameInput = usernameEditText.text.toString().trim()
            if (isUsernameTaken(usernameInput)) {
                Toast.makeText(
                    baseContext,
                    "Username already taken",
                    Toast.LENGTH_SHORT,
                ).show()
                usernameEditText.error = "Username already taken"
                emailEditText.error = if (emailInput.isEmpty()) "Field cannot be empty" else null
                passwordEditText.error = if (passwordInput.isEmpty()) "Field cannot be empty" else null
                confirmPasswordEditText.error = if (confirmPasswordInput.isEmpty()) "Field cannot be empty" else null
            }
            else if (usernameInput.isEmpty()) {
                usernameEditText.error = "Field cannot be empty"
                emailEditText.error = if (emailInput.isEmpty()) "Field cannot be empty" else null
                passwordEditText.error = if (passwordInput.isEmpty()) "Field cannot be empty" else null
                confirmPasswordEditText.error = if (confirmPasswordInput.isEmpty()) "Field cannot be empty" else null
            }
            if (emailInput.isEmpty()) {
                emailEditText.error = "Field cannot be empty"
                passwordEditText.error = if (passwordInput.isEmpty()) "Field cannot be empty" else null
                confirmPasswordEditText.error = if (confirmPasswordInput.isEmpty()) "Field cannot be empty" else null
            }
            else if(passwordInput.isEmpty()) {
                passwordEditText.error = "Field cannot be empty"
                confirmPasswordEditText.error = if (confirmPasswordInput.isEmpty()) "Field cannot be empty" else null
            }
            else if(confirmPasswordInput.isEmpty()) {
                confirmPasswordEditText.error = "Field cannot be empty"
            }
            else if(passwordInput != confirmPasswordInput) {
                confirmPasswordEditText.error = "Passwords don't match"
            }
            else {
                auth.createUserWithEmailAndPassword(emailInput, confirmPasswordInput)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val userDoc = hashMapOf(
                                "email" to emailInput,
                                "username" to usernameInput
                            )
                            firestoreDB.collection("registered_users").document(emailInput)
                                .set(userDoc)
                                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }


                            Log.d(TAG, "createUserWithEmail:success")
                            val user = auth.currentUser
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                                emailEditText.error = "Invalid email format"
                                Toast.makeText(
                                    baseContext,
                                    "Invalid email format",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                Log.w(TAG, "createUserWithEmail:failure", task.exception)
                                Toast.makeText(
                                    baseContext,
                                    "Authentication failed",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
            }
        }
    }

    private fun isUsernameTaken(username: String): Boolean {
        val docRef = firestoreDB.collection("registered_users").document(username)
        var docRefExists = false
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    docRefExists = true
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
        return docRefExists
    }
}

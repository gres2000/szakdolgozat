package com.taskraze.myapplication.view.authentication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.taskraze.myapplication.view.main.MainActivity
import com.taskraze.myapplication.R
import com.taskraze.myapplication.databinding.LoginActivityBinding
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.taskraze.myapplication.common.UserPreferences
import com.taskraze.myapplication.model.authentication.EncryptionHelper

class LoginActivity: AppCompatActivity() {
    private lateinit var binding: LoginActivityBinding
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewModel: MainViewModel
    private lateinit var auth: FirebaseAuth
    private val firestoreDB = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserPreferences.isUserLoggedIn(this)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            viewModel = ViewModelProvider(this)[MainViewModel::class.java]

            binding = LoginActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)
            loginButton = findViewById(R.id.buttonLogin)
            registerButton = findViewById(R.id.buttonRegister)
            emailEditText = findViewById(R.id.editTextEmail)
            passwordEditText = findViewById(R.id.editTextPassword)

            auth = Firebase.auth

            sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val isFirstTime = sharedPreferences.getBoolean("isFirstTime", true)

            if (isFirstTime) {
                EncryptionHelper.generateKey()

                val editor = sharedPreferences.edit()
                editor.putBoolean("isFirstTime", false)
                editor.apply()
            }

            loginButton.setOnClickListener {
                val emailInput = emailEditText.text.toString().trim()
                val passwordInput = passwordEditText.text.toString().trim()
                if (emailInput.isEmpty()) {
                    emailEditText.error = "Field cannot be empty"
                    passwordEditText.error = if (passwordInput.isEmpty()) "Field cannot be empty" else null
                }
                else if(passwordInput.isEmpty()) {
                    passwordEditText.error = "Field cannot be empty"
                }
                else {
                    auth.signInWithEmailAndPassword(emailInput, passwordInput)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                // Sign in success, update UI with the signed-in user's information
                                val user = auth.currentUser
                                //set user logged in for later
                                UserPreferences.setUserLoggedIn(this, true)


                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()

                            } else {
                                // If sign in fails, display a message to the user.
                                Toast.makeText(
                                    baseContext,
                                    "Authentication failed.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                }
            }

            registerButton.setOnClickListener {
                val emailInput = emailEditText.text.toString().trim()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.putExtra("Email", emailInput)
                startActivity(intent)
            }
        }
        /*viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loginButton = findViewById(R.id.buttonLogin)
        registerButton = findViewById(R.id.buttonRegister)
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)

        auth = Firebase.auth

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val isFirstTime = sharedPreferences.getBoolean("isFirstTime", true)

        if (isFirstTime) {
            EncryptionHelper.generateKey()

            val editor = sharedPreferences.edit()
            editor.putBoolean("isFirstTime", false)
            editor.apply()
        }

        loginButton.setOnClickListener {
            val emailInput = emailEditText.text.toString().trim()
            val passwordInput = passwordEditText.text.toString().trim()
            if (emailInput.isEmpty()) {
                emailEditText.error = "Field cannot be empty"
                passwordEditText.error = if (passwordInput.isEmpty()) "Field cannot be empty" else null
            }
            else if(passwordInput.isEmpty()) {
                passwordEditText.error = "Field cannot be empty"
            }
            else {
                *//*val userDoc = hashMapOf(
                    "email" to emailInput,
                    "username" to passwordInput

                )
                firestoreDB.collection("registered_users").document(emailInput)
                    .set(userDoc)
                    .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
*//*
                auth.signInWithEmailAndPassword(emailInput, passwordInput)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            val user = auth.currentUser
                            //set user logged in for later
                            UserPreferences.setUserLoggedIn(this, true)

                            viewModel.authenticateUser()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()

                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(
                                baseContext,
                                "Authentication failed.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
            }
        }

        registerButton.setOnClickListener {
            val emailInput = emailEditText.text.toString().trim()
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("Email", emailInput)
            startActivity(intent)
        }*/
    }
}
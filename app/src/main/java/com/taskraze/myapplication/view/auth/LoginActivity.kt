package com.taskraze.myapplication.view.auth

import AuthViewModelFactory
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.taskraze.myapplication.R
import com.taskraze.myapplication.databinding.LoginActivityBinding
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import com.taskraze.myapplication.common.UserPreferences
import com.taskraze.myapplication.model.auth.EncryptionHelper
import com.taskraze.myapplication.view.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginActivityBinding
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (UserPreferences.isUserLoggedIn(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loginButton = findViewById(R.id.buttonLogin)
        registerButton = findViewById(R.id.buttonRegister)
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)

        auth = Firebase.auth

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("isFirstTime", true)) {
            EncryptionHelper.generateKey()
            sharedPreferences.edit().putBoolean("isFirstTime", false).apply()
        }

        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(this)
        )[AuthViewModel::class.java]

        loginButton.setOnClickListener {
            val emailInput = emailEditText.text.toString().trim()
            val passwordInput = passwordEditText.text.toString().trim()

            if (emailInput.isEmpty()) {
                emailEditText.error = "Field cannot be empty"
                if (passwordInput.isEmpty()) passwordEditText.error = "Field cannot be empty"
                return@setOnClickListener
            } else if (passwordInput.isEmpty()) {
                passwordEditText.error = "Field cannot be empty"
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(emailInput, passwordInput)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        UserPreferences.setUserLoggedIn(this, true)
                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                        authViewModel.fetchAndCacheUser(userId) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }

                    } else {
                        emailEditText.error = null
                        passwordEditText.error = null

                        val genericError = "Wrong email or password"
                        emailEditText.error = genericError
                        passwordEditText.error = genericError
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
}

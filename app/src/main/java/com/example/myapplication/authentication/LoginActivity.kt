package com.example.myapplication.authentication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.LoginActivityBinding
import com.example.myapplication.viewModel.MainViewModel

class LoginActivity: AppCompatActivity() {
    private lateinit var binding: LoginActivityBinding
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loginButton = findViewById(R.id.buttonLogin)
        registerButton = findViewById(R.id.buttonRegister)
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

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
                viewModel.authenticateUser()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
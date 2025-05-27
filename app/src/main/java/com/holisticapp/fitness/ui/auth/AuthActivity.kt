package com.holisticapp.fitness.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.holisticapp.fitness.R
import com.holisticapp.fitness.ui.main.MainActivity

class AuthActivity : AppCompatActivity() {
    
    private lateinit var tabLayout: TabLayout
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var fullNameEditText: EditText
    private lateinit var authButton: Button
    
    private var isSignInMode = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        
        initViews()
        setupTabLayout()
        setupAuthButton()
    }
    
    private fun initViews() {
        tabLayout = findViewById(R.id.tabLayout)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        fullNameEditText = findViewById(R.id.fullNameEditText)
        authButton = findViewById(R.id.authButton)
    }
    
    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Sign In"))
        tabLayout.addTab(tabLayout.newTab().setText("Register"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> switchToSignIn()
                    1 -> switchToRegister()
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun switchToSignIn() {
        isSignInMode = true
        fullNameEditText.visibility = android.view.View.GONE
        confirmPasswordEditText.visibility = android.view.View.GONE
        authButton.text = "Sign In"
    }
    
    private fun switchToRegister() {
        isSignInMode = false
        fullNameEditText.visibility = android.view.View.VISIBLE
        confirmPasswordEditText.visibility = android.view.View.VISIBLE
        authButton.text = "Register"
    }
    
    private fun setupAuthButton() {
        authButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!isSignInMode) {
                val fullName = fullNameEditText.text.toString().trim()
                val confirmPassword = confirmPasswordEditText.text.toString().trim()
                
                if (fullName.isEmpty()) {
                    Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (password != confirmPassword) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // For demo purposes, show success message for registration
                Toast.makeText(this, "Registration successful! Logging you in...", Toast.LENGTH_SHORT).show()
            } else {
                // For demo purposes, show success message for sign in
                Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show()
            }
            
            // Navigate to MainActivity regardless of input (for demo)
            navigateToMainActivity()
        }
    }
    
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
} 
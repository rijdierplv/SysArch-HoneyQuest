package com.example.honeyquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.database.database

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.EmailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val forgotPasswordButton = findViewById<Button>(R.id.forgotPasswordButton)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loginButton.setOnClickListener {

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emailKey = email.replace(".", "_")

            val database = Firebase.database
            val userRef = database.getReference("users").child(emailKey)

            userRef.get()
                .addOnSuccessListener { snapshot ->

                    if (!snapshot.exists()) {
                        Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val savedPassword = snapshot.child("password").getValue(String::class.java)

                    if (password == savedPassword) {

                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                        val sharedPref = getSharedPreferences("HoneyQuestPrefs", MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("current_user", email)
                        editor.apply()

                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()

                    } else {
                        Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                }
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        forgotPasswordButton.setOnClickListener {
            startActivity(Intent(this, ForgotPassActivity::class.java))
            finish()
        }
    }
}
package com.example.honeyquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.database.database

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        val logout = findViewById<Button>(R.id.logoutButton)
        val usernameTextView = findViewById<TextView>(R.id.usernameTextView)
        val homeButton = findViewById<ImageButton>(R.id.btnHome)

        val sharedPref = getSharedPreferences("HoneyQuestPrefs", MODE_PRIVATE)
        val currentUser = sharedPref.getString("current_user", null)

        if (currentUser != null) {
            val database = Firebase.database
            val userRef = database.getReference("users").child(currentUser)

            userRef.get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val username = snapshot.child("username").getValue(String::class.java) ?: currentUser
                        usernameTextView.text = username
                    } else {
                        usernameTextView.text = "No user data found"
                    }
                }
                .addOnFailureListener {
                    usernameTextView.text = currentUser
                }
        } else {
            usernameTextView.text = "No user logged in"
        }

        logout.setOnClickListener {
            val editor = sharedPref.edit()
            editor.remove("current_user")
            editor.apply()

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        homeButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
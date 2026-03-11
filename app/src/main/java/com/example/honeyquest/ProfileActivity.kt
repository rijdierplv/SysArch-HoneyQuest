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

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        val logout=findViewById<Button>(R.id.logoutButton)
        val sharedPref = getSharedPreferences("HoneyQuestPrefs", MODE_PRIVATE)
        val currentUser = sharedPref.getString("current_user", null)

        val usernameTextView = findViewById<TextView>(R.id.usernameTextView)
        val homeButton=findViewById<ImageButton>(R.id.btnHome)
        if (currentUser != null) {
            val password = sharedPref.getString("user_$currentUser", "N/A")

            usernameTextView.text = currentUser
        } else {
            usernameTextView.text = "No user logged in"
        }
        logout.setOnClickListener {
            val intent=Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        homeButton.setOnClickListener {
            val intent=Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
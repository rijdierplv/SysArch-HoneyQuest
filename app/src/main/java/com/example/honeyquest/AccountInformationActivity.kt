package com.example.honeyquest

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.database.database

class AccountInformationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_information)

        // Views
        val backButton          = findViewById<ImageButton>(R.id.backButton)
        val usernameValueText   = findViewById<TextView>(R.id.usernameValueText)
        val emailValueText      = findViewById<TextView>(R.id.emailValueText)

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.accountInformation)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ── Load current user ─────────────────────────────────────────────────
        val sharedPref = getSharedPreferences("HoneyQuestPrefs", MODE_PRIVATE)
        val currentUserEmail = sharedPref.getString("current_user", null)

        if (currentUserEmail == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val emailKey = currentUserEmail.replace(".", "_")
        val database = Firebase.database
        val userRef  = database.getReference("users").child(emailKey)

        // ── Fetch and display account info ────────────────────────────────────
        userRef.get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.child("username").getValue(String::class.java)
                val email    = snapshot.child("email").getValue(String::class.java)

                usernameValueText.text = username ?: "N/A"
                emailValueText.text    = email    ?: "N/A"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load account information", Toast.LENGTH_SHORT).show()
            }

        // ── Back Button ───────────────────────────────────────────────────────
        backButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}
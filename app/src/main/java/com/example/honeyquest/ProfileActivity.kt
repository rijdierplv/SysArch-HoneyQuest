package com.example.honeyquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.database.database

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val accountSecurityButton = findViewById<Button>(R.id.accountSecurityButton)
        val accountInfoButton     = findViewById<Button>(R.id.accountInfoButton)
        val editProfileTextView   = findViewById<TextView>(R.id.editProfileTextView)
        val logout                = findViewById<Button>(R.id.logoutButton)
        val usernameTextView      = findViewById<TextView>(R.id.usernameTextView)

        val sharedPref  = getSharedPreferences("HoneyQuestPrefs", MODE_PRIVATE)
        val currentUser = sharedPref.getString("current_user", null)

        if (currentUser != null) {
            val emailKey = currentUser.replace(".", "_")
            val database = Firebase.database
            val userRef  = database.getReference("users").child(emailKey)

            userRef.get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val username = snapshot.child("username")
                            .getValue(String::class.java) ?: "Unknown User"
                        usernameTextView.text = username
                    } else {
                        usernameTextView.text = "No user data found"
                    }
                }
                .addOnFailureListener {
                    usernameTextView.text = "Failed to load user"
                }
        } else {
            usernameTextView.text = "No user logged in"
        }

        logout.setOnClickListener {
            val editor = sharedPref.edit()
            editor.remove("current_user")
            editor.apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        editProfileTextView.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
            finish()
        }
        accountInfoButton.setOnClickListener {
            startActivity(Intent(this, AccountInformationActivity::class.java))
            finish()
        }
        accountSecurityButton.setOnClickListener {
            startActivity(Intent(this, AccountSecurityActivity::class.java))
            finish()
        }

        setupBottomNav()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupBottomNav() {
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.hivesBtn).setOnClickListener {
            startActivity(Intent(this, HiveManagement::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.inspectionBtn).setOnClickListener {
            startActivity(Intent(this, InspectionActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.harvestLogsBtn).setOnClickListener {
            startActivity(Intent(this, HarvestLogsActivity::class.java))
            finish()
        }
        // Already on Profile — no-op
        findViewById<ImageButton>(R.id.profileButton).setOnClickListener { }
    }
}
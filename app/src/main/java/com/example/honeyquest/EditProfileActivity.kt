package com.example.honeyquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.database.database

class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Views
        val backButton    = findViewById<ImageButton>(R.id.backButton)
        val usernameInput = findViewById<EditText>(R.id.editUsernameInput)
        val emailInput    = findViewById<EditText>(R.id.editEmailInput)
        val saveButton    = findViewById<Button>(R.id.saveButton)

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editProfile)) { v, insets ->
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
        val usersRef = database.getReference("users")
        val userRef  = usersRef.child(emailKey)

        // ── Pre-fill current username and email ───────────────────────────────
        userRef.get()
            .addOnSuccessListener { snapshot ->
                usernameInput.setText(snapshot.child("username").getValue(String::class.java) ?: "")
                emailInput.setText(snapshot.child("email").getValue(String::class.java) ?: "")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }

        // ── Save Changes ──────────────────────────────────────────────────────
        saveButton.setOnClickListener {
            val newUsername = usernameInput.text.toString().trim()
            val newEmail    = emailInput.text.toString().trim()

            // Basic validation
            if (newUsername.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidUsername(newUsername)) {
                Toast.makeText(
                    this,
                    "Username must be 3-20 characters and contain only letters, numbers, or underscore (_)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newEmailKey    = newEmail.replace(".", "_")
            val emailChanged   = newEmail != currentUserEmail

            // ── Step 1: Check username uniqueness ─────────────────────────────
            usersRef.orderByChild("username").equalTo(newUsername).get()
                .addOnSuccessListener { usernameSnapshot ->
                    val takenByOther = usernameSnapshot.children.any { it.key != emailKey }
                    if (takenByOther) {
                        Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    if (!emailChanged) {
                        // ── Email not changed: only update username ───────────
                        userRef.child("username").setValue(newUsername)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, ProfileActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // ── Step 2: Email changed — check new email is free ───
                        usersRef.child(newEmailKey).get()
                            .addOnSuccessListener { emailSnapshot ->
                                if (emailSnapshot.exists()) {
                                    Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                // ── Step 3: Read full data, write to new key, delete old key ──
                                userRef.get()
                                    .addOnSuccessListener { currentSnapshot ->
                                        val password = currentSnapshot.child("password")
                                            .getValue(String::class.java) ?: ""

                                        val updatedData = mapOf(
                                            "email"    to newEmail,
                                            "username" to newUsername,
                                            "password" to password
                                        )

                                        usersRef.child(newEmailKey).setValue(updatedData)
                                            .addOnSuccessListener {
                                                // Delete old record
                                                userRef.removeValue()
                                                    .addOnSuccessListener {
                                                        // Update SharedPreferences to new email
                                                        sharedPref.edit()
                                                            .putString("current_user", newEmail)
                                                            .apply()

                                                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                                        startActivity(Intent(this, ProfileActivity::class.java))
                                                        finish()
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(this, "Failed to remove old record", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Failed to fetch current data", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error checking email", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error checking username", Toast.LENGTH_SHORT).show()
                }
        }

        // ── Back Button ───────────────────────────────────────────────────────
        backButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun isValidUsername(username: String): Boolean {
        return username.matches(Regex("^[A-Za-z0-9_]{3,20}$"))
    }
}
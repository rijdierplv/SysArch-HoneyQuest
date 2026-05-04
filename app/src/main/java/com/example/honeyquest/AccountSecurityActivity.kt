package com.example.honeyquest

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.database.database

class AccountSecurityActivity : AppCompatActivity() {

    private var isCurrentPassVisible = false
    private var isNewPassVisible     = false
    private var isConfirmPassVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_security)

        // Views
        val backButton              = findViewById<ImageButton>(R.id.backButton)
        val currentPasswordInput    = findViewById<EditText>(R.id.currentPasswordInput)
        val newPasswordInput        = findViewById<EditText>(R.id.newPasswordInput)
        val confirmPasswordInput    = findViewById<EditText>(R.id.confirmNewPasswordInput)
        val toggleCurrentPassBtn    = findViewById<ImageButton>(R.id.toggleCurrentPassBtn)
        val toggleNewPassBtn        = findViewById<ImageButton>(R.id.toggleNewPassBtn)
        val toggleConfirmPassBtn    = findViewById<ImageButton>(R.id.toggleConfirmPassBtn)
        val changePasswordButton    = findViewById<Button>(R.id.changePasswordButton)

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.accountSecurity)) { v, insets ->
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

        // ── Toggle Password Visibility ────────────────────────────────────────
        toggleCurrentPassBtn.setOnClickListener {
            isCurrentPassVisible = !isCurrentPassVisible
            currentPasswordInput.inputType = if (isCurrentPassVisible)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleCurrentPassBtn.setImageResource(
                if (isCurrentPassVisible) R.drawable.eye_open else R.drawable.eye_closed
            )
            currentPasswordInput.setSelection(currentPasswordInput.text.length)
        }

        toggleNewPassBtn.setOnClickListener {
            isNewPassVisible = !isNewPassVisible
            newPasswordInput.inputType = if (isNewPassVisible)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleNewPassBtn.setImageResource(
                if (isNewPassVisible) R.drawable.eye_open else R.drawable.eye_closed
            )
            newPasswordInput.setSelection(newPasswordInput.text.length)
        }

        toggleConfirmPassBtn.setOnClickListener {
            isConfirmPassVisible = !isConfirmPassVisible
            confirmPasswordInput.inputType = if (isConfirmPassVisible)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleConfirmPassBtn.setImageResource(
                if (isConfirmPassVisible) R.drawable.eye_open else R.drawable.eye_closed
            )
            confirmPasswordInput.setSelection(confirmPasswordInput.text.length)
        }

        // ── Change Password ───────────────────────────────────────────────────
        changePasswordButton.setOnClickListener {
            val currentPassword = currentPasswordInput.text.toString().trim()
            val newPassword     = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            // Validation
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword == currentPassword) {
                Toast.makeText(this, "New password must be different from current password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPassword(newPassword)) {
                Toast.makeText(
                    this,
                    "Password must be 8-20 characters, include a letter, number, special character, and no spaces",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Verify current password from Firebase then update
            userRef.get()
                .addOnSuccessListener { snapshot ->
                    val savedPassword = snapshot.child("password").getValue(String::class.java)

                    if (currentPassword != savedPassword) {
                        Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Update password
                    userRef.child("password").setValue(newPassword)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                            currentPasswordInput.text.clear()
                            newPasswordInput.text.clear()
                            confirmPasswordInput.text.clear()
                            startActivity(Intent(this, ProfileActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error verifying current password", Toast.LENGTH_SHORT).show()
                }
        }

        // ── Back Button ───────────────────────────────────────────────────────
        backButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        if (password.length > 20) return false
        if (password.contains(" ")) return false
        val hasLetter  = password.any { it.isLetter() }
        val hasDigit   = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasLetter && hasDigit && hasSpecial
    }
}
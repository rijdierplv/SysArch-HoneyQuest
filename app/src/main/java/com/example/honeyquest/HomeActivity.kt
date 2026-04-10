package com.example.honeyquest

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        val prof=findViewById<ImageButton>(R.id.profileButton)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val hivesBtn = findViewById<ImageButton>(R.id.hivesBtn)
        prof.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        hivesBtn.setOnClickListener {
            startActivity(Intent(this, HiveManagement::class.java))
            finish()
        }
    }
}
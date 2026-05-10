package com.example.honeyquest

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var hivesRef: DatabaseReference
    private lateinit var harvestRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val currentUser = getSharedPreferences("HoneyQuestPrefs", MODE_PRIVATE)
            .getString("current_user", null)

        if (currentUser.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val emailKey = currentUser.replace(".", "_")
        hivesRef   = Firebase.database.getReference("hives").child(emailKey)
        harvestRef = Firebase.database.getReference("harvest_logs").child(emailKey)

        setupBottomNav()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        // ── Hive stats + alerts ───────────────────────────────────────────────
        hivesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalHives       = 0
                var readyToHarvest   = 0
                var needsInspection  = 0
                var overdue          = 0
                val alerts           = mutableListOf<String>()

                val sdf       = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val cal       = Calendar.getInstance()
                val weekStart = cal.clone() as Calendar
                weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                weekStart.set(Calendar.HOUR_OF_DAY, 0)
                weekStart.set(Calendar.MINUTE, 0)
                weekStart.set(Calendar.SECOND, 0)
                weekStart.set(Calendar.MILLISECOND, 0)

                var inspectionsThisWeek = 0

                for (child in snapshot.children) {
                    val hive = child.getValue(HivesModel::class.java)
                    if (hive != null) {
                        totalHives++
                        when (hive.status) {
                            "Ready to Harvest" -> {
                                readyToHarvest++
                                alerts.add("🍯 \"${hive.name}\" is Ready for Harvest")
                            }
                            "Needs Inspection" -> {
                                needsInspection++
                                alerts.add("🔍 \"${hive.name}\" Needs Inspection")
                            }
                            "Overdue" -> {
                                overdue++
                                alerts.add("🚨 \"${hive.name}\" Inspection Overdue")
                            }
                        }

                        // Count inspections this week
                        if (!hive.inspectionDate.isNullOrEmpty() && hive.inspectionDate != "Not set") {
                            try {
                                val inspDate = sdf.parse(hive.inspectionDate)
                                if (inspDate != null && inspDate.after(weekStart.time) || inspDate == weekStart.time) {
                                    inspectionsThisWeek++
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }

                // Update stat views
                findViewById<TextView>(R.id.tvTotalHives).text    = totalHives.toString()
                findViewById<TextView>(R.id.tvReadyHarvest).text  = readyToHarvest.toString()
                findViewById<TextView>(R.id.tvInspections).text   = "This Week: $inspectionsThisWeek"

                // Update alerts
                val alertContainer = findViewById<LinearLayout>(R.id.alertContainer)
                alertContainer.removeAllViews()

                if (alerts.isEmpty()) {
                    val noAlerts = TextView(this@HomeActivity).apply {
                        text = "No active alerts. All good!"
                        setTextColor(Color.parseColor("#888888"))
                        gravity = android.view.Gravity.CENTER
                        setPadding(8, 8, 8, 8)
                        textSize = 13f
                    }
                    alertContainer.addView(noAlerts)
                } else {
                    for (alert in alerts) {
                        val alertView = TextView(this@HomeActivity).apply {
                            text = alert
                            setTextColor(Color.parseColor("#2D2D2D"))
                            textSize = 13f
                            setPadding(4, 6, 4, 6)
                        }
                        alertContainer.addView(alertView)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // ── Harvest stats ─────────────────────────────────────────────────────
        harvestRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalHoney = 0.0
                for (child in snapshot.children) {
                    val log = child.getValue(HarvestLogModel::class.java)
                    if (log != null) {
                        totalHoney += log.honeyAmount
                    }
                }
                findViewById<TextView>(R.id.tvTotalHoney).text = "${"%.1f".format(totalHoney)} kg"

                val tvTotalHarvested = findViewById<TextView>(R.id.tvTotalHarvested)
                tvTotalHarvested?.text = "Total harvested: ${"%.1f".format(totalHoney)} kg"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupBottomNav() {
        // Already on Home — no-op
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener { }

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
        findViewById<ImageButton>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}
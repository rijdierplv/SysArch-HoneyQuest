package com.example.honeyquest

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.*

class InspectionActivity : AppCompatActivity() {

    private lateinit var inspectionContainer: LinearLayout
    private lateinit var hivesRef: DatabaseReference
    private lateinit var harvestRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inspection)

        inspectionContainer = findViewById(R.id.inspectionContainer)

        val currentUser = getSharedPreferences("HoneyQuestPrefs", MODE_PRIVATE)
            .getString("current_user", null)

        if (currentUser.isNullOrEmpty()) {
            Toast.makeText(this, "No logged in user found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val emailKey = currentUser.replace(".", "_")
        hivesRef    = Firebase.database.getReference("hives").child(emailKey)
        harvestRef  = Firebase.database.getReference("harvest_logs").child(emailKey)

        setupBottomNav()
        loadHives()
    }

    private fun loadHives() {
        hivesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                inspectionContainer.removeAllViews()
                val hives = mutableListOf<HivesModel>()

                for (child in snapshot.children) {
                    val hive = child.getValue(HivesModel::class.java)
                    if (hive != null) {
                        if (hive.hiveId.isNullOrEmpty()) hive.hiveId = child.key
                        hives.add(hive)
                    }
                }

                if (hives.isEmpty()) {
                    val emptyText = TextView(this@InspectionActivity).apply {
                        text = "No hives registered.\nAdd a hive to start inspecting!"
                        textSize = 14f
                        setTextColor(resources.getColor(R.color.black, null))
                        gravity = Gravity.CENTER
                        setPadding(0, 80, 0, 0)
                    }
                    inspectionContainer.addView(emptyText)
                } else {
                    for (hive in hives) {
                        addInspectionCard(hive)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@InspectionActivity, "Failed to load hives", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addInspectionCard(hive: HivesModel) {
        val cardView = LayoutInflater.from(this)
            .inflate(R.layout.item_inspection_hive, inspectionContainer, false)

        val tvHiveName = cardView.findViewById<TextView>(R.id.tvHiveName)
        val tvHiveCode = cardView.findViewById<TextView>(R.id.tvHiveCode)
        val tvStatus   = cardView.findViewById<TextView>(R.id.tvStatus)
        val btnAction  = cardView.findViewById<Button>(R.id.btnAction)

        tvHiveName.text = hive.name ?: "Unnamed Hive"
        tvHiveCode.text = hive.code ?: ""

        val status = hive.status ?: "Healthy"
        tvStatus.text = status

        // Status color
        val statusColor = when (status) {
            "Ready to Harvest"  -> Color.parseColor("#E65100")
            "Needs Inspection"  -> Color.parseColor("#1565C0")
            "Overdue"           -> Color.parseColor("#C62828")
            else                -> Color.parseColor("#2E7D32")
        }
        tvStatus.setTextColor(statusColor)

        // Action button
        when (status) {
            "Ready to Harvest" -> {
                btnAction.text = "Harvest Now"
                btnAction.setOnClickListener { showHarvestConfirmDialog(hive) }
            }
            "Needs Inspection", "Overdue" -> {
                btnAction.text = "Inspect Now"
                btnAction.setOnClickListener { showInspectConfirmDialog(hive) }
            }
            else -> {
                btnAction.text = "✓ Healthy"
                btnAction.isEnabled = false
                btnAction.alpha = 0.5f
            }
        }

        cardView.translationY = 60f
        cardView.alpha = 0f
        cardView.animate().translationY(0f).alpha(1f).setDuration(250).start()

        inspectionContainer.addView(cardView)
    }

    // ── Harvest Flow ──────────────────────────────────────────────────────────
    private fun showHarvestConfirmDialog(hive: HivesModel) {
        val honeyAmt = hive.frameCount * hive.honeyPerFrame
        AlertDialog.Builder(this)
            .setTitle("Harvest Hive")
            .setMessage(
                "Are you sure you want to harvest \"${hive.name}\"?\n\n" +
                        "This will log ${"%.1f".format(honeyAmt)} kg of honey."
            )
            .setPositiveButton("Yes") { _, _ -> performHarvest(hive) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performHarvest(hive: HivesModel) {
        val logId      = harvestRef.push().key ?: return
        val honeyAmt   = hive.frameCount * hive.honeyPerFrame
        val sdf        = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val today      = sdf.format(Date())

        val log = HarvestLogModel(
            logId         = logId,
            hiveId        = hive.hiveId,
            hiveName      = hive.name,
            hiveCode      = hive.code,
            honeyAmount   = honeyAmt,
            harvestDate   = today,
            harvestMethod = hive.harvestMethod ?: "Bucket",
            notes         = ""
        )

        harvestRef.child(logId).setValue(log)
            .addOnSuccessListener {
                // Reset hive status to Healthy after harvest
                hive.hiveId?.let { id ->
                    hivesRef.child(id).child("status").setValue("Healthy")
                }
                Toast.makeText(
                    this,
                    "Hive harvested! ${"%.1f".format(honeyAmt)} kg logged.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to log harvest", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Inspect Flow ──────────────────────────────────────────────────────────
    private fun showInspectConfirmDialog(hive: HivesModel) {
        AlertDialog.Builder(this)
            .setTitle("Inspect Hive")
            .setMessage("Are you sure you want to mark \"${hive.name}\" as inspected?")
            .setPositiveButton("Yes") { _, _ -> performInspection(hive) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performInspection(hive: HivesModel) {
        val sdf   = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val today = sdf.format(Date())

        val updates = mapOf(
            "inspectionDate" to today,
            "status" to "Healthy"
        )

        hive.hiveId?.let { id ->
            hivesRef.child(id).updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Hive inspected successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update inspection", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // ── Bottom Navigation ─────────────────────────────────────────────────────
    private fun setupBottomNav() {
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.hivesBtn).setOnClickListener {
            startActivity(Intent(this, HiveManagement::class.java))
            finish()
        }
        // Already on Inspection — no-op
        findViewById<ImageButton>(R.id.inspectionBtn).setOnClickListener { }
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
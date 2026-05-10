package com.example.honeyquest

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
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

class HarvestLogsActivity : AppCompatActivity() {

    private lateinit var harvestContainer: LinearLayout
    private lateinit var harvestRef: DatabaseReference

    data class AggregatedHarvest(
        var hiveId: String = "",
        var hiveName: String = "",
        var hiveCode: String = "",
        var totalHoney: Double = 0.0,
        var harvestCount: Int = 0,
        var lastHarvestDate: String = "",
        var harvestMethod: String = "Bucket"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_harvest_logs)

        harvestContainer = findViewById(R.id.harvestContainer)

        val currentUser = getSharedPreferences("HoneyQuestPrefs", MODE_PRIVATE)
            .getString("current_user", null)

        if (currentUser.isNullOrEmpty()) {
            Toast.makeText(this, "No logged in user found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val emailKey = currentUser.replace(".", "_")
        harvestRef = Firebase.database.getReference("harvest_logs").child(emailKey)

        setupBottomNav()
        loadHarvestLogs()
    }

    private fun loadHarvestLogs() {
        harvestRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                harvestContainer.removeAllViews()

                val aggregated = mutableMapOf<String, AggregatedHarvest>()

                for (child in snapshot.children) {
                    val log = child.getValue(HarvestLogModel::class.java)
                    if (log != null) {
                        val key = log.hiveId ?: continue
                        val existing = aggregated[key]
                        if (existing == null) {
                            aggregated[key] = AggregatedHarvest(
                                hiveId = key,
                                hiveName = log.hiveName ?: "Unknown",
                                hiveCode = log.hiveCode ?: "",
                                totalHoney = log.honeyAmount,
                                harvestCount = 1,
                                lastHarvestDate = log.harvestDate ?: "",
                                harvestMethod = log.harvestMethod ?: "Bucket"
                            )
                        } else {
                            existing.totalHoney += log.honeyAmount
                            existing.harvestCount++
                            if ((log.harvestDate ?: "") > existing.lastHarvestDate) {
                                existing.lastHarvestDate = log.harvestDate ?: ""
                            }
                        }
                    }
                }

                if (aggregated.isEmpty()) {
                    val emptyText = TextView(this@HarvestLogsActivity).apply {
                        text = "No harvest logs yet.\nHarvest a hive to see logs here!"
                        textSize = 14f
                        setTextColor(resources.getColor(R.color.black, null))
                        gravity = Gravity.CENTER
                        setPadding(0, 80, 0, 0)
                    }
                    harvestContainer.addView(emptyText)
                } else {
                    for ((_, data) in aggregated) {
                        addHarvestCard(data)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HarvestLogsActivity, "Failed to load harvest logs", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addHarvestCard(data: AggregatedHarvest) {
        val cardView = LayoutInflater.from(this)
            .inflate(R.layout.item_harvest_log, harvestContainer, false)

        val tvHiveName     = cardView.findViewById<TextView>(R.id.tvHiveName)
        val tvTotalHoney   = cardView.findViewById<TextView>(R.id.tvTotalHoney)
        val tvHarvestDate  = cardView.findViewById<TextView>(R.id.tvHarvestDate)
        val tvStatus       = cardView.findViewById<TextView>(R.id.tvStatus)
        val tvHarvestCount = cardView.findViewById<TextView>(R.id.tvHarvestCount)

        tvHiveName.text     = data.hiveName
        tvTotalHoney.text   = "Total: ${"%.1f".format(data.totalHoney)} kg"
        tvHarvestCount.text = "${data.harvestCount} harvest(s)  ·  ${data.harvestMethod}"
        tvHarvestDate.text  = if (data.lastHarvestDate.isNotEmpty())
            "📅 Last: ${data.lastHarvestDate}" else ""
        tvStatus.text       = "Harvested"

        cardView.translationY = 60f
        cardView.alpha = 0f
        cardView.animate().translationY(0f).alpha(1f).setDuration(250).start()

        harvestContainer.addView(cardView)
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
        // Already on Harvest Logs — no-op
        findViewById<ImageButton>(R.id.harvestLogsBtn).setOnClickListener { }
        findViewById<ImageButton>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}
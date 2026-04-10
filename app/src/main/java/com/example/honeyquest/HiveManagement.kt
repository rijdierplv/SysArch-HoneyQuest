package com.example.honeyquest

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.database.*
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.*

class HiveManagement : AppCompatActivity() {

    private lateinit var hiveContainer: LinearLayout
    private lateinit var hivesNumber: TextView
    private lateinit var hivesRef: DatabaseReference

    private val hiveTypes = listOf("Langstroth", "Top Bar", "Warre", "Flow Hive")
    private val statusLabels = listOf("Healthy", "Ready to Harvest", "Needs Inspection", "Overdue")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hive_management)

        hiveContainer = findViewById(R.id.hiveContainer)
        hivesNumber = findViewById(R.id.hivesNumber)

        val currentUser = getSharedPreferences("HoneyQuestPrefs", MODE_PRIVATE)
            .getString("current_user", null)

        if (currentUser.isNullOrEmpty()) {
            Toast.makeText(this, "No logged in user found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val emailKey = currentUser.replace(".", "_")
        hivesRef = Firebase.database.getReference("hives").child(emailKey)

        findViewById<View>(R.id.btnAddHive).setOnClickListener {
            showHiveBottomSheet()
        }

        loadHives()
    }

    private fun loadHives() {
        hivesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                hiveContainer.removeAllViews()

                val loadedHives = mutableListOf<HivesModel>()

                for (child in snapshot.children) {
                    val hive = child.getValue(HivesModel::class.java)
                    if (hive != null) {
                        if (hive.hiveId.isNullOrEmpty()) {
                            hive.hiveId = child.key
                        }
                        loadedHives.add(hive)
                    }
                }

                for (hive in loadedHives.asReversed()) {
                    addHiveCard(hive)
                }

                updateCount(loadedHives.size)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@HiveManagement,
                    "Failed to load hives: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showHiveBottomSheet(existingHive: HivesModel? = null) {
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.add_hive, null)
        dialog.setContentView(sheetView)

        val tvDialogTitle = sheetView.findViewById<TextView>(R.id.tvDialogTitle)
        val etName = sheetView.findViewById<TextInputEditText>(R.id.etHiveName)
        val etCode = sheetView.findViewById<TextInputEditText>(R.id.etHiveCode)
        val spinnerType = sheetView.findViewById<Spinner>(R.id.spinnerHiveType)
        val etLocation = sheetView.findViewById<TextInputEditText>(R.id.etLocation)
        val btnHarvestDate = sheetView.findViewById<Button>(R.id.btnHarvestDate)
        val btnInspDate = sheetView.findViewById<Button>(R.id.btnLastInspection)
        val spinnerStatus = sheetView.findViewById<Spinner>(R.id.spinnerStatus)
        val btnSave = sheetView.findViewById<Button>(R.id.btnAddHive)
        val btnCancel = sheetView.findViewById<Button>(R.id.btnCancel)

        spinnerType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            hiveTypes
        )

        spinnerStatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statusLabels
        )

        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        var harvestDate = ""
        var inspectionDate = ""

        if (existingHive == null) {
            tvDialogTitle.text = "Add New Hive"
            btnSave.text = "Add Hive"
        } else {
            tvDialogTitle.text = "Edit Hive"
            btnSave.text = "Update Hive"

            etName.setText(existingHive.name ?: "")
            etCode.setText(existingHive.code ?: "")
            etLocation.setText(existingHive.location ?: "")

            setSpinnerSelection(spinnerType, hiveTypes, existingHive.hiveType)
            setSpinnerSelection(spinnerStatus, statusLabels, existingHive.status)

            val savedHarvest = existingHive.harvestDate ?: ""
            val savedInspection = existingHive.inspectionDate ?: ""

            if (savedHarvest.isNotEmpty() && savedHarvest != "Not set") {
                harvestDate = savedHarvest
                btnHarvestDate.text = savedHarvest
            }

            if (savedInspection.isNotEmpty() && savedInspection != "Not set") {
                inspectionDate = savedInspection
                btnInspDate.text = savedInspection
            }
        }

        btnHarvestDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    cal.set(y, m, d)
                    harvestDate = sdf.format(cal.time)
                    btnHarvestDate.text = harvestDate
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnInspDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    cal.set(y, m, d)
                    inspectionDate = sdf.format(cal.time)
                    btnInspDate.text = inspectionDate
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val code = etCode.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val hiveType = spinnerType.selectedItem.toString()
            val status = spinnerStatus.selectedItem.toString()

            if (name.isEmpty() || code.isEmpty() || location.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please fill in all required fields (*)",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val finalHarvestDate = if (harvestDate.isBlank()) "Not set" else harvestDate
            val finalInspectionDate = if (inspectionDate.isBlank()) "Not set" else inspectionDate

            if (existingHive == null) {
                saveNewHive(
                    name = name,
                    code = code,
                    hiveType = hiveType,
                    location = location,
                    harvestDate = finalHarvestDate,
                    inspectionDate = finalInspectionDate,
                    status = status,
                    dialog = dialog
                )
            } else {
                val hiveId = existingHive.hiveId
                if (hiveId.isNullOrEmpty()) {
                    Toast.makeText(this, "Invalid hive ID", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                updateHive(
                    hiveId = hiveId,
                    name = name,
                    code = code,
                    hiveType = hiveType,
                    location = location,
                    harvestDate = finalHarvestDate,
                    inspectionDate = finalInspectionDate,
                    status = status,
                    dialog = dialog
                )
            }
        }

        dialog.show()
    }

    private fun saveNewHive(
        name: String,
        code: String,
        hiveType: String,
        location: String,
        harvestDate: String,
        inspectionDate: String,
        status: String,
        dialog: BottomSheetDialog
    ) {
        val hiveId = hivesRef.push().key

        if (hiveId == null) {
            Toast.makeText(this, "Failed to generate hive ID", Toast.LENGTH_SHORT).show()
            return
        }

        val hive = HivesModel(
            hiveId = hiveId,
            name = name,
            code = code,
            hiveType = hiveType,
            location = location,
            harvestDate = harvestDate,
            inspectionDate = inspectionDate,
            status = status
        )

        hivesRef.child(hiveId).setValue(hive)
            .addOnSuccessListener {
                Toast.makeText(this, "Hive added successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add hive", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateHive(
        hiveId: String,
        name: String,
        code: String,
        hiveType: String,
        location: String,
        harvestDate: String,
        inspectionDate: String,
        status: String,
        dialog: BottomSheetDialog
    ) {
        val updatedHive = HivesModel(
            hiveId = hiveId,
            name = name,
            code = code,
            hiveType = hiveType,
            location = location,
            harvestDate = harvestDate,
            inspectionDate = inspectionDate,
            status = status
        )

        hivesRef.child(hiveId).setValue(updatedHive)
            .addOnSuccessListener {
                Toast.makeText(this, "Hive updated successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update hive", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDeleteHive(hive: HivesModel) {
        val hiveId = hive.hiveId
        if (hiveId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid hive ID", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Hive")
            .setMessage("Are you sure you want to delete ${hive.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteHive(hiveId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteHive(hiveId: String) {
        hivesRef.child(hiveId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Hive deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete hive", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addHiveCard(hive: HivesModel) {
        val cardView = LayoutInflater.from(this)
            .inflate(R.layout.item_hive, hiveContainer, false)

        val hiveName = cardView.findViewById<TextView>(R.id.tvHiveName)
        val hiveCode = cardView.findViewById<TextView>(R.id.tvHiveCode)
        val location = cardView.findViewById<TextView>(R.id.tvLocation)
        val harvestDate = cardView.findViewById<TextView>(R.id.tvHarvestDate)
        val inspectionDate = cardView.findViewById<TextView>(R.id.tvLastInspection)
        val statusView = cardView.findViewById<TextView>(R.id.tvStatus)
        val btnEdit = cardView.findViewById<Button>(R.id.btnEditHive)
        val btnDelete = cardView.findViewById<Button>(R.id.btnDeleteHive)

        hiveName.text = hive.name ?: "Unnamed Hive"

        val codeText = if (!hive.code.isNullOrBlank() && !hive.hiveType.isNullOrBlank()) {
            "${hive.code} · ${hive.hiveType}"
        } else {
            hive.code ?: "No Code"
        }
        hiveCode.text = codeText

        location.text = hive.location ?: "—"
        harvestDate.text = hive.harvestDate ?: "Not set"
        inspectionDate.text = hive.inspectionDate ?: "Not set"

        val statusLabel = hive.status ?: "Healthy"
        statusView.text = statusLabel
        statusView.backgroundTintList = ColorStateList.valueOf(statusColorFor(statusLabel))

        btnEdit.setOnClickListener {
            showHiveBottomSheet(hive)
        }

        btnDelete.setOnClickListener {
            confirmDeleteHive(hive)
        }

        cardView.translationY = 60f
        cardView.alpha = 0f
        cardView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(250)
            .start()

        hiveContainer.addView(cardView)
    }

    private fun updateCount(count: Int) {
        hivesNumber.text = if (count == 0) {
            "No hives registered"
        } else {
            "$count hives registered"
        }
    }

    private fun setSpinnerSelection(spinner: Spinner, items: List<String>, value: String?) {
        val index = items.indexOf(value)
        if (index >= 0) {
            spinner.setSelection(index)
        }
    }

    private fun statusColorFor(label: String): Int {
        return when (label) {
            "Ready to Harvest" -> ContextCompat.getColor(this, R.color.status_harvest)
            "Needs Inspection" -> ContextCompat.getColor(this, R.color.status_inspection)
            "Overdue" -> ContextCompat.getColor(this, R.color.status_overdue)
            else -> ContextCompat.getColor(this, R.color.status_healthy)
        }
    }
}
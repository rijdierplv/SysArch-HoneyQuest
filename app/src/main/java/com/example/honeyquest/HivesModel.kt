package com.example.honeyquest

data class HivesModel(
    var hiveId: String? = null,
    var name: String? = null,
    var code: String? = null,
    var hiveType: String? = null,
    var location: String? = null,
    var harvestDate: String? = null,
    var inspectionDate: String? = null,
    var status: String? = null,
    var frameCount: Int = 0,           // Number of bee frames in the hive
    var honeyPerFrame: Double = 0.0,   // kg of honey per frame
    var harvestMethod: String? = null  // e.g. "Bucket"
)
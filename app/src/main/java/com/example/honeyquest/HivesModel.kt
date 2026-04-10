package com.example.honeyquest

data class HivesModel(
    var hiveId: String? = null,
    var name: String? = null,
    var code: String? = null,
    var hiveType: String? = null,
    var location: String? = null,
    var harvestDate: String? = null,
    var inspectionDate: String? = null,
    var status: String? = null
)
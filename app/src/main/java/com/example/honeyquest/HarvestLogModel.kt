package com.example.honeyquest

data class HarvestLogModel(
    var logId: String? = null,
    var hiveId: String? = null,
    var hiveName: String? = null,
    var hiveCode: String? = null,
    var honeyAmount: Double = 0.0,
    var harvestDate: String? = null,
    var harvestMethod: String? = null,
    var notes: String? = null
)
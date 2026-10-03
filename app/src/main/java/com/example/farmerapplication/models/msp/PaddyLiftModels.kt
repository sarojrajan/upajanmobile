package com.example.farmerapplication.models.msp

data class SavePaddyLiftRecordsRequest(
    val mspCentreId: String,
    val roNumber: String,
    val vehicle_entries: List<VehicleEntry>
)

data class VehicleEntry(
    val vehicle_number: String,
    val quantity: String,
    val image_base64: String,
    val latitude: String,
    val longitude: String
)

data class SavePaddyLiftRecordsResponse(
    val status_code: Int?,
    val message: String?
)
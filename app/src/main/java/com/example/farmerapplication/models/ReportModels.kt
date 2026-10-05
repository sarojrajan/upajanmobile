package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

// ---- Today's Scheduled Farmers ----
data class ScheduledFarmerResponse(
    @SerializedName("status_code") val statusCode: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<ScheduledFarmerItem>
)

data class ScheduledFarmerItem(
    @SerializedName("farmer_Id") val farmerId: String,
    @SerializedName("farmerName") val farmerName: String,
    @SerializedName("schedule_Date") val scheduleDate: String,
    @SerializedName("est_Qty") val estQty: Double
)

// ---- Commodity Received (Paddy Given) ----
data class CommodityReceivedResponse(
    @SerializedName("status_code") val statusCode: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<CommodityReceivedItem>
)

data class CommodityReceivedItem(
    @SerializedName("farmer_Id") val farmerId: String,
    @SerializedName("farmerName") val farmerName: String,
    @SerializedName("qtyReceived") val qtyReceived: Double,
    @SerializedName("date_Of_Receipt") val dateOfReceipt: String
)
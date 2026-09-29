package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

data class FarmerSlotBookingRequest(

    @SerializedName("district_id")
    val districtId: Int,

    @SerializedName("farmer_id")
    val farmerId: String,

    @SerializedName("farmer_name")
    val farmerName: String,

    @SerializedName("mobile_no")
    val mobileNo: String,

    @SerializedName("mspcenter_place_name")
    val mspCenterPlaceName: String,

    @SerializedName("schedule_date")
    val scheduleDate: String
)

data class FarmerSlotBookingResponse(

    @SerializedName("status_code")
    val statusCode: Int? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("schedule_date")
    val scheduleDate: String? = null,

    @SerializedName("estimated_quantity")
    val estimatedQuantity: Double? = null
)
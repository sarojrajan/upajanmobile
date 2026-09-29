package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

data class FarmerRegistrationRequest(

    @SerializedName("district_code")
    val districtCode: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("mobile")
    val mobile: String,

    @SerializedName("aadhaar")
    val aadhaar: String,

    @SerializedName("password")
    val password: String
)

data class FarmerRegistrationResponse(

    @SerializedName("status_code")
    val statusCode: Int?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("farmer_id")
    val farmerId: String?,

    @SerializedName("ackno")
    val acknowledgementNumber: String?
)
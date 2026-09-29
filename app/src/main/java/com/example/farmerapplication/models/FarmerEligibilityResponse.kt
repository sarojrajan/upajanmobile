package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

data class FarmerEligibilityResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("estimated_quantity") val estimatedQuantity: Double? = null,
    @SerializedName("is_KarmachariVerify") val isKarmachariVerify: Int? = null,
    @SerializedName("is_COVerify") val isCOVerify: Int? = null,
    @SerializedName("is_DSOVerify") val isDSOVerify: Int? = null
)
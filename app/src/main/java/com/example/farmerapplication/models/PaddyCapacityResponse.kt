package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

data class PaddyCapacityResponse(
    @SerializedName("totalPaddy") val totalPaddy: Double? = null,
    @SerializedName("paddyLiftByMiller") val paddyLiftByMiller: Double? = null,
    @SerializedName("warehousePaddy") val warehousePaddy: Double? = null,
    @SerializedName("targetPaddy") val targetPaddy: Double? = null,
    @SerializedName("remainingCapacity") val remainingCapacity: Double? = null
)
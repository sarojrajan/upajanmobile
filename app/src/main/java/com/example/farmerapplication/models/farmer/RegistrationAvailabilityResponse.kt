package com.example.farmerapplication.models.farmer

import com.google.gson.annotations.SerializedName

data class RegistrationAvailabilityResponse(

    @SerializedName("status_code")
    val statusCode: Int? = null,

    @SerializedName("message")
    val message: String? = null
)
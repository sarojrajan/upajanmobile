package com.example.farmerapplication.models.farmer

import com.google.gson.annotations.SerializedName

data class GenerateOtpRequest(

    @SerializedName("mobile")
    val mobile: String
)

data class GenerateOtpResponse(

    @SerializedName("status_code")
    val statusCode: Int,

    @SerializedName("message")
    val message: String
)
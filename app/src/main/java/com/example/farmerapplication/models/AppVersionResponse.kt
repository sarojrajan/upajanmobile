package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

data class AppVersionResponse(
    @SerializedName("status_code") val statusCode: Int?,
    @SerializedName("version") val version: String?
)
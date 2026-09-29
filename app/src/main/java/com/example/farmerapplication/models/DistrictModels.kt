package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

data class DistrictResponse(

    @SerializedName("status_code")
    val statusCode: Int? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<District>? = emptyList()
)

data class District(

    @SerializedName("district_id")
    val districtId: Int? = null,

    @SerializedName("district_code")
    val districtCode: String? = null,

    @SerializedName("district_name")
    val districtName: String? = null
)

/**
 * Used when the API returns an error response.
 */
data class ApiErrorResponse(

    @SerializedName("status_code")
    val statusCode: Int? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("errors")
    val errors: Map<String, List<String>>? = null
)
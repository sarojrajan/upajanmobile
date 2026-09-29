package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

data class MSPCenterDetailsResponse(

    @SerializedName("status_code") val statusCode: Int?,

    @SerializedName("msp_center_name") val mspCenterName: String?
)
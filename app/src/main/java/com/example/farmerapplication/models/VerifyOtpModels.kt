package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

data class VerifyOtpRequest(

    @SerializedName("mobile")
    val mobile: String,

    @SerializedName("otp")
    val otp: Int
)

data class VerifyOtpResponse(

    @SerializedName("status_code")
    val statusCode: Int = 0,

    @SerializedName("message")
    val message: String = "",

    @SerializedName("user_id")
    val userId: Int = 0,

    @SerializedName("farmer_name")
    val farmerName: String = "",

    @SerializedName("farmer_id")
    val farmerId: String = "",

    @SerializedName("district_id")
    val districtId: String = "",

    @SerializedName("district_name")
    val districtName: String = "",

    @SerializedName("msp_center_id")
    val mspCenterId: String = "",

    @SerializedName("msp_center_name")
    val mspCenterName: String = "",

    @SerializedName("mobile_no")
    val mobileNo: String = "",

    @SerializedName("aadhar_last_6")
    val aadharLast6: String = "",

    @SerializedName("basic_detail")
    val basicDetail: Int = 0,

    @SerializedName("land_detail")
    val landDetail: Int = 0
)
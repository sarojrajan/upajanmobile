package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

// Request model for MSP login
data class MSPLoginRequest(
    @SerializedName("email_id") val emailId: String,
    @SerializedName("password") val password: String
)

// Response model for MSP login
data class MSPLoginResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("MSP_Centre_id") val mspCentreId: Long? = null,
    @SerializedName("MSP_Centre_name") val mspCentreName: String? = null,
    @SerializedName("email_id") val emailId: String? = null,
    @SerializedName("District_ID") val districtId: Long? = null,
    @SerializedName("District_Name") val districtName: String? = null,
    @SerializedName("ManagerName") val managerName: String? = null,
    @SerializedName("MgrAdhar") val managerAadhaar: String? = null,
    @SerializedName("MgrMobileNo") val managerMobileNo: String? = null,
    @SerializedName("AccNO") val accountNumber: String? = null,
    @SerializedName("IFSC_Code") val ifscCode: String? = null,
    @SerializedName("BranchName") val branchName: String? = null,
    @SerializedName("bco_name") val bcoName: String? = null,
    @SerializedName("bco_mobile") val bcoMobile: String? = null,
    @SerializedName("SocBandaranCapacity") val mspCapacity: Int? = null
)


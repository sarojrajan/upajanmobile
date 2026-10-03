package com.example.farmerapplication.models.miller

data class MillerLoginRequest(
    val email_id: String,
    val password: String
)

data class MillerLoginResponse(
    val status_code: Int,
    val message: String?,
    val Miller_id: Int?,
    val Miller_name: String?,
    val District_Name: String?,
    val email_id: String?,
    val mobile_no: String?,
    val TINNO: String?,
    val PAN: String?,
    val LicenseNO: String?,
    val Bank: String?,
    val BranchName: String?,
    val AccountNo: String?,
    val IFSCCode: String?,
    val miller_head_name: String?,
    val mt_per_hour: Int?
)

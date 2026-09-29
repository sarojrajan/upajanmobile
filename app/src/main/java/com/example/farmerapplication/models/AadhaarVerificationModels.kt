package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

/** Request used for generating Aadhaar OTP. */
data class AadhaarSendOtpRequest(
    @SerializedName("aadhaar_number") val aadhaarNumber: String
)

/** Request used for verifying Aadhaar OTP using transaction ID. */
data class AadhaarVerifyOtpRequest(
    @SerializedName("aadhaar_number") val aadhaarNumber: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("transaction_id") val transactionId: String
)

/** Response model containing required fields for Aadhaar OTP API operations. */
data class AadhaarOtpResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("aadhaar_verified") val aadhaarVerified: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("transaction_id") val transactionId: String? = null,
    @SerializedName("aadhaar_error_code") val aadhaarErrorCode: String? = null,
    @SerializedName("aadhaar_api_response") val aadhaarApiResponse: String? = null
)
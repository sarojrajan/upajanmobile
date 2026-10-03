package com.example.farmerapplication.models.farmer

import com.google.gson.annotations.SerializedName

data class FarmerProfileDetailsResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: FarmerProfileDetailsData? = null
)

data class FarmerProfileDetailsData(
    @SerializedName("fatherHusName") val fatherHusName: String? = null,
    @SerializedName("subdistrict_name") val subdistrictName: String? = null,
    @SerializedName("panchayat") val panchayat: String? = null,
    @SerializedName("villageName") val villageName: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("farmer_BankName_New") val bankName: String? = null,
    @SerializedName("branch") val branch: String? = null,
    @SerializedName("farmer_BankAccountNo") val accountNumber: String? = null,
    @SerializedName("ifsC_CODE") val ifscCode: String? = null
)
package com.example.farmerapplication.models

import com.google.gson.annotations.SerializedName

data class FarmerDetailsResponse(
    @SerializedName("district_Id") val districtId: String? = null,
    @SerializedName("district_Name") val districtName: String? = null,
    @SerializedName("subdistrict_Code") val subdistrictCode: String? = null,
    @SerializedName("subdistrict_Name") val subdistrictName: String? = null,
    @SerializedName("panchayat_Name") val gramPanchayat: String? = null,
    @SerializedName("village_Id") val villageId: String? = null,
    @SerializedName("villageName") val villageName: String? = null,
    @SerializedName("farmer_Id") val farmerId: String? = null,
    @SerializedName("farmerName") val farmerName: String? = null,
    @SerializedName("fatherHusName") val fatherHusName: String? = null,
    @SerializedName("mobileno") val mobileno: String? = null,
    @SerializedName("farmer_BankAccountNo") val farmerBankAccountNo: String? = null,
    @SerializedName("mspCenter_Place") val mspCenterPlace: String? = null,
    @SerializedName("mspCenter_Place_Name") val mspCenterPlaceName: String? = null,
    @SerializedName("procured_qty") val procuredQty: Double? = null,
    @SerializedName("qtyReceived") val qtyReceived: Double? = null,
    @SerializedName("stockAval") val stockAval: Double? = null
)

data class SendFarmerSmsRequest(
    @SerializedName("farmerId") val farmerId: String,
    @SerializedName("districtId") val districtId: String,
    @SerializedName("farmerName") val farmerName: String,
    @SerializedName("fatherHusName") val fatherHusName: String,
    @SerializedName("mobileno") val mobileno: String,
    @SerializedName("mspCenterPlace") val mspCenterPlace: String,
    @SerializedName("mspCenterPlaceName") val mspCenterPlaceName: String,
    @SerializedName("accountNoForSms") val accountNoForSms: String,
    @SerializedName("estQty") val estQty: Double
)

data class SendFarmerSmsResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("serverResponse") val serverResponse: String? = null,
    @SerializedName("smsSent") val smsSent: Boolean? = null
)
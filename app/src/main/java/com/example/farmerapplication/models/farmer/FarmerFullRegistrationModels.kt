package com.example.farmerapplication.models.farmer

import com.google.gson.annotations.SerializedName

// BLOCK
data class BlockResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<BlockItem>? = emptyList()
)

data class BlockItem(
    @SerializedName("subdistrict_code") val subdistrictCode: String? = null,
    @SerializedName("subdistrict_name") val subdistrictName: String? = null
)

// PANCHAYAT
data class PanchayatResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<PanchayatItem>? = emptyList()
)

data class PanchayatItem(
    @SerializedName("local_body_code") val localBodyCode: String? = null,
    @SerializedName("panchayat") val panchayatName: String? = null
)

// VILLAGE
data class VillageResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<VillageItem>? = emptyList()
)

data class VillageItem(
    @SerializedName("village_code") val villageCode: String? = null,
    @SerializedName("village_name") val villageName: String? = null
)

// CATEGORY
data class CategoryItem(
    val categoryName: String,
    val categoryValue: String
)

// BANK
data class BankResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<BankItem>? = emptyList()
)

data class BankItem(
    @SerializedName("bank_id") val bankId: String? = null,
    @SerializedName("bank_name") val bankName: String? = null
)

// BRANCH
data class BankDetailsResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<BankBranchItem>? = emptyList()
)

data class BankBranchItem(
    @SerializedName(value = "branch_Id", alternate = ["branch_id", "branchId"]) val branchId: String? = null,
    @SerializedName(value = "ifsC_Code", alternate = ["ifsc_code", "ifscCode", "IFSC_Code"]) val ifscCode: String? = null,
    @SerializedName(value = "branch_Name", alternate = ["branch_name", "branchName"]) val branchName: String? = null
)

// CIRCLE
data class CircleResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<CircleItem>? = emptyList()
)

data class CircleItem(
    @SerializedName("circle_code") val circleCode: String? = null,
    @SerializedName("circle_name") val circleName: String? = null
)

// HALKA
data class HalkaResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<HalkaItem>? = emptyList()
)

data class HalkaItem(
    @SerializedName("halka_code") val halkaCode: String? = null,
    @SerializedName("halka_name") val halkaName: String? = null
)

// FULL REGISTRATION
data class FarmerFullRegistrationRequest(
    @SerializedName("farmerId") val farmerId: String,
    @SerializedName("farmerName") val farmerName: String,
    @SerializedName("fatherHusName") val fatherHusName: String,
    @SerializedName("mobile") val mobile: String,
    @SerializedName("aadhaar") val aadhaar: String,
    @SerializedName("category") val category: String,
    @SerializedName("halkaNo") val halkaNo: String,
    @SerializedName("districtId") val districtId: String,
    @SerializedName("blockId") val blockId: String,
    @SerializedName("panchayatId") val panchayatId: String,
    @SerializedName("villageId") val villageId: String,
    @SerializedName("villageName") val villageName: String,
    @SerializedName("bankId") val bankId: String,
    @SerializedName("bankName") val bankName: String,
    @SerializedName("branchId") val branchId: String,
    @SerializedName("ifsc") val ifsc: String,
    @SerializedName("accountNo") val accountNo: String,
    @SerializedName("aadhaarBase64") val aadhaarBase64: String,
    @SerializedName("bankBase64") val bankBase64: String
)

data class FarmerFullRegistrationResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null
)
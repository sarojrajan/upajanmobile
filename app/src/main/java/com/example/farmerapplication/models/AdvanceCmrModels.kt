package com.example.farmerapplication.models

// ---------- Districts ----------
data class MillerDistrictItem(
    val districtCode: String,
    val districtName: String
)

// ---------- JSFC Godowns ----------
data class MillerJsfcGodownItem(
    val jsfcId: String,
    val depoName: String
)

// ---------- Commodity Types ----------
data class CommodityTypeItem(
    val id: Int,
    val name: String
)

// ---------- Vehicle Types ----------
data class VehicleTypeItem(
    val id: String,
    val vehicleType: String
)

// ---------- Gunny Bag Year ----------
data class GunnyBagYearItem(
    val id: String,
    val gunnyBagYear: String
)

// ---------- Gunny Bag Type ----------
data class GunnyBagTypeItem(
    val id: String,
    val gunnyBagType: String
)

// ---------- Rice Submit Request ----------
data class BagDetail(
    val vehicleId: String,
    val vehicleType: String,
    val vehicleNumber: String,
    val driverName: String,
    val gunnyBagId: String,
    val gunnyBagYear: String,
    val gunnyBagType: String,
    val netWeight: String,
    val totalBags: String,
    val mobileNo: String
)

data class RiceSubmitRequest(
    val millerId: String,
    val riceSubmitted: Int,
    val dateToSubmitted: String,
    val transDistrictCode: String,
    val jsfcGodownId: String,
    val stancilingCode: String,
    val commodityTypeId: Int,
    val bankGuaranteeCMR: Int,
    val bagDetails: List<BagDetail>
)

data class RiceSubmitResponse(
    val success: Boolean,
    val message: String?,
    val lotNo: String?,
    val noOfLots: Int?
)
package com.example.farmerapplication.models

data class FarmerLandDetailsResponse(
    val statusCode: Int?,
    val message: String?,
    val data: List<FarmerLandDetail>?
)

data class FarmerLandDetail(
    val landType: String?,
    val circleName: String?,
    val halkaName: String?,
    val maujaName: String?,
    val landOwnerName: String?,
    val landOwnerRinPustikaNo: String?,
    val plotNo: String?,
    val khasaraNo: String?,
    val crop: String?,
    val rakba: Double?,
    val rakbaCropSinchit: Double?,
    val rakbaCropAsinchit: Double?,
    val isKarmachariVerify: Int?,
    val isCOVerify: Int?,
    val isDSOVerify: Int?
)
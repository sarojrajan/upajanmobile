package com.example.farmerapplication.models.farmer

import com.google.gson.annotations.SerializedName

// ---------------------------------------------------------
// MAUJA DROPDOWN
// ---------------------------------------------------------

data class MaujaResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<MaujaItem>? = emptyList()
)

data class MaujaItem(
    @SerializedName(value = "mauja_code", alternate = ["mauja_id", "maujaCode", "MaujaCode"]) val maujaCode: String? = null,
    @SerializedName(value = "mauja_name", alternate = ["mauja", "maujaName", "MaujaName"]) val maujaName: String? = null
)

// ---------------------------------------------------------
// SAVE LAND RECORD REQUEST
// ---------------------------------------------------------

data class SaveLandRecordsRequest(
    @SerializedName("farmer_id") val farmerId: String,
    @SerializedName("land_entries") val landEntries: List<LandRecordEntry>,
    @SerializedName("land_doc_base64") val landDocumentBase64: String
)

data class LandRecordEntry(
    @SerializedName("subdistrict_code") val subdistrictCode: String,

    /* As requested: Volume No in REG II is sent as land_owner_name. */
    @SerializedName("land_owner_name") val landOwnerName: String,

    /* As requested: Page No in REG II is sent as land_owner_rin_pustika_no. */
    @SerializedName("land_owner_rin_pustika_no") val landOwnerRinPustikaNo: String,

    @SerializedName("land_type") val landType: String,

    /* Khata No is sent as khasara_no because the supplied API request does not contain a separate khata_no property. */
    @SerializedName("khasara_no") val khasaraNo: String,

    @SerializedName("rakba") val rakba: String,
    @SerializedName("rakba_crop_sinchit") val irrigatedLand: String,
    @SerializedName("rakba_crop_asinchit") val unirrigatedLand: String,
    @SerializedName("rakba_crop_sinchit_qty") val irrigatedQuantity: String,
    @SerializedName("rakba_crop_asinchit_qty") val unirrigatedQuantity: String,
    @SerializedName("procured_qty") val procuredQuantity: String = "0",
    @SerializedName("circle_id") val circleId: String,
    @SerializedName("circle_name") val circleName: String,
    @SerializedName("mauja_id") val maujaId: String,
    @SerializedName("mauja_name") val maujaName: String,
    @SerializedName("halka_id") val halkaId: String,
    @SerializedName("halka_name") val halkaName: String,
    @SerializedName("plot_no") val plotNo: String,
    @SerializedName("crop") val crop: String
)

// ---------------------------------------------------------
// SAVE LAND RECORD RESPONSE
// ---------------------------------------------------------

data class SaveLandRecordsResponse(
    @SerializedName("status_code") val statusCode: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: Any? = null
)
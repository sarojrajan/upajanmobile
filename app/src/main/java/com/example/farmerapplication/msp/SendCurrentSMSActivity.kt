package com.example.farmerapplication.msp

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.FarmerDetailsResponse
import com.example.farmerapplication.models.SendFarmerSmsRequest
import com.example.farmerapplication.models.SendFarmerSmsResponse
import android.app.AlertDialog
import com.example.farmerapplication.models.FarmerEligibilityResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class SendCurrentSMSActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DISTRICT_ID = "extra_district_id"
        const val EXTRA_SOC_ID = "extra_soc_id"
    }

    // Search
    private lateinit var edtFarmerId: EditText
    private lateinit var btnSearchFarmer: Button
    private lateinit var progressSearchFarmer: ProgressBar

    // Farmer details
    private lateinit var llFarmerDetails: View
    private lateinit var txtDistrict: TextView
    private lateinit var txtBlock: TextView
    private lateinit var txtPanchayat: TextView
    private lateinit var txtVillage: TextView
    private lateinit var txtFarmerId: TextView
    private lateinit var txtFarmerName: TextView
    private lateinit var txtFatherHusbandName: TextView
    private lateinit var txtMobileNo: TextView
    private lateinit var txtBankAccountNo: TextView
    private lateinit var txtMspCentreId: TextView
    private lateinit var txtMspCentreName: TextView
    private lateinit var txtProcuredQuantity: TextView
    private lateinit var txtSubmittedQuantity: TextView
    private lateinit var txtAvailableQuantity: TextView
    private lateinit var txtAvailableHint: TextView

    // Quantity / SMS
    private lateinit var edtQuantityToBeReceived: EditText
    private lateinit var btnSendCurrentSMS: Button
    private lateinit var progressSendCurrentSMS: ProgressBar

    private var districtIdFromIntent = ""
    private var socIdFromIntent = ""

    private var farmerDetails: FarmerDetailsResponse? = null
    private var stockAvailable: Double = 0.0

    private var searchCall: Call<FarmerDetailsResponse>? = null
    private var smsCall: Call<SendFarmerSmsResponse>? = null

    private var eligibilityCall: Call<FarmerEligibilityResponse>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msp_current_slot_booking_layout)

        districtIdFromIntent = intent.getStringExtra(EXTRA_DISTRICT_ID).orEmpty()
        socIdFromIntent = intent.getStringExtra(EXTRA_SOC_ID).orEmpty()

        if (districtIdFromIntent.isBlank() || socIdFromIntent.isBlank()) {
            Toast.makeText(this, "MSP information is unavailable.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        edtFarmerId = findViewById(R.id.edtFarmerId)
        btnSearchFarmer = findViewById(R.id.btnSearchFarmer)
        progressSearchFarmer = findViewById(R.id.progressSearchFarmer)

        llFarmerDetails = findViewById(R.id.llFarmerDetails)
        txtDistrict = findViewById(R.id.txtDistrict)
        txtBlock = findViewById(R.id.txtBlock)
        txtPanchayat = findViewById(R.id.txtPanchayat)
        txtVillage = findViewById(R.id.txtVillage)
        txtFarmerId = findViewById(R.id.txtFarmerId)
        txtFarmerName = findViewById(R.id.txtFarmerName)
        txtFatherHusbandName = findViewById(R.id.txtFatherHusbandName)
        txtMobileNo = findViewById(R.id.txtMobileNo)
        txtBankAccountNo = findViewById(R.id.txtBankAccountNo)
        txtMspCentreId = findViewById(R.id.txtMspCentreId)
        txtMspCentreName = findViewById(R.id.txtMspCentreName)
        txtProcuredQuantity = findViewById(R.id.txtProcuredQuantity)
        txtSubmittedQuantity = findViewById(R.id.txtSubmittedQuantity)
        txtAvailableQuantity = findViewById(R.id.txtAvailableQuantity)
        txtAvailableHint = findViewById(R.id.txtAvailableHint)

        edtQuantityToBeReceived = findViewById(R.id.edtQuantityToBeReceived)
        btnSendCurrentSMS = findViewById(R.id.btnSendCurrentSMS)
        progressSendCurrentSMS = findViewById(R.id.progressSendCurrentSMS)
    }

    private fun setupListeners() {
        btnSearchFarmer.setOnClickListener {
            val farmerIdText = edtFarmerId.text.toString().trim()
            if (farmerIdText.isEmpty()) {
                edtFarmerId.error = "Please enter Farmer ID"
                edtFarmerId.requestFocus()
                return@setOnClickListener
            }
            hideKeyboard()
            searchFarmer(farmerIdText)
        }

        btnSendCurrentSMS.setOnClickListener {
            hideKeyboard()
            val quantity = validateQuantity() ?: return@setOnClickListener
            sendCurrentSMS(quantity)
        }
    }

    /** Returns the validated quantity, or null after showing an error on the field. */
    private fun validateQuantity(): Double? {
        val quantityText = edtQuantityToBeReceived.text.toString().trim()

        if (quantityText.isEmpty()) {
            edtQuantityToBeReceived.error = "Please enter quantity"
            edtQuantityToBeReceived.requestFocus()
            return null
        }

        val quantity = quantityText.toDoubleOrNull()
        if (quantity == null || quantity <= 0) {
            edtQuantityToBeReceived.error = "Enter a valid quantity"
            edtQuantityToBeReceived.requestFocus()
            return null
        }

        if (quantity > stockAvailable) {
            edtQuantityToBeReceived.error =
                "Quantity cannot be more than available quantity (${formatQty(stockAvailable)})"
            edtQuantityToBeReceived.requestFocus()
            return null
        }
        return quantity
    }

    // API 1 (eligibility)
    private fun searchFarmer(farmerId: String) {
        eligibilityCall?.cancel()
        searchCall?.cancel()
        setSearchLoading(true)
        llFarmerDetails.visibility = View.GONE
        farmerDetails = null

        eligibilityCall = RetrofitClient.apiService.checkFarmerEligibility(
            farmerId = farmerId,
            districtId = districtIdFromIntent,
            mspCenterId = socIdFromIntent
        )
        eligibilityCall?.enqueue(object : Callback<FarmerEligibilityResponse> {
            override fun onResponse(
                call: Call<FarmerEligibilityResponse>,
                response: Response<FarmerEligibilityResponse>
            ) {
                if (isFinishing || isDestroyed) return

                val body = response.body()
                if (response.isSuccessful && body?.status == 1) {
                    // Eligible -> continue with getFarmerDetails
                    fetchFarmerDetails(farmerId)
                } else {
                    setSearchLoading(false)
                    // status = 0 (body) or 409 etc. (error body) -> show message in dialog
                    val message = body?.message?.takeIf { it.isNotBlank() }
                        ?: parseEligibilityMessage(response.errorBody()?.string())
                        ?: when (response.code()) {
                            in 500..599 -> "Server error occurred. Please try again."
                            else -> "Unable to verify farmer eligibility."
                        }
                    showEligibilityDialog(message)
                }
            }

            override fun onFailure(call: Call<FarmerEligibilityResponse>, t: Throwable) {
                if (isFinishing || isDestroyed || call.isCanceled) return
                setSearchLoading(false)
                Toast.makeText(this@SendCurrentSMSActivity, networkMessage(t), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showEligibilityDialog(message: String) {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Not Eligible")
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish() // back to the previous page
            }
            .show()
    }

    /** Reads "message" from error JSON such as {"status_code":409,"message":"..."} */
    private fun parseEligibilityMessage(errorJson: String?): String? = try {
        if (errorJson.isNullOrBlank()) null
        else Gson().fromJson(errorJson, FarmerEligibilityResponse::class.java)
            ?.message?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    //API 2 (farmer details)
    private fun fetchFarmerDetails(farmerId: String) {
        searchCall?.cancel()

        searchCall = RetrofitClient.apiService.getFarmerDetails(
            farmerId = farmerId,
            districtId = districtIdFromIntent,
            socId = socIdFromIntent
        )
        searchCall?.enqueue(object : Callback<FarmerDetailsResponse> {
            override fun onResponse(call: Call<FarmerDetailsResponse>, response: Response<FarmerDetailsResponse>) {
                if (isFinishing || isDestroyed) return
                setSearchLoading(false)

                val body = response.body()
                if (response.isSuccessful && body != null && !body.farmerId.isNullOrBlank()) {
                    farmerDetails = body
                    showFarmerDetails(body)
                } else {
                    val message = parseMessage(response.errorBody()?.string())
                        ?: when (response.code()) {
                            400 -> "Invalid Farmer ID."
                            404 -> "Farmer not found."
                            in 500..599 -> "Server error occurred. Please try again."
                            else -> "Unable to fetch farmer details."
                        }
                    Toast.makeText(this@SendCurrentSMSActivity, message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<FarmerDetailsResponse>, t: Throwable) {
                if (isFinishing || isDestroyed || call.isCanceled) return
                setSearchLoading(false)
                Toast.makeText(this@SendCurrentSMSActivity, networkMessage(t), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showFarmerDetails(d: FarmerDetailsResponse) {
        txtDistrict.text = clean(d.districtName)
        txtBlock.text = clean(d.subdistrictName)
        txtPanchayat.text = clean(d.gramPanchayat)
        txtVillage.text = clean(d.villageName)

        txtFarmerId.text = clean(d.farmerId)
        txtFarmerName.text = clean(d.farmerName)
        txtFatherHusbandName.text = clean(d.fatherHusName)
        txtMobileNo.text = clean(d.mobileno)
        txtBankAccountNo.text = clean(d.farmerBankAccountNo)

        txtMspCentreId.text = clean(d.mspCenterPlace)
        txtMspCentreName.text = clean(d.mspCenterPlaceName)

        stockAvailable = d.stockAval ?: 0.0

        txtProcuredQuantity.text = formatQty(d.procuredQty ?: 0.0)
        txtSubmittedQuantity.text = formatQty(d.qtyReceived ?: 0.0)
        txtAvailableQuantity.text = formatQty(stockAvailable)

        txtAvailableHint.text = "${formatQty(stockAvailable)} quintal maximum"

        edtQuantityToBeReceived.text?.clear()
        edtQuantityToBeReceived.error = null

        val canSend = stockAvailable > 0
        edtQuantityToBeReceived.isEnabled = canSend
        btnSendCurrentSMS.isEnabled = canSend
        btnSendCurrentSMS.alpha = if (canSend) 1f else 0.5f
        if (!canSend) {
            Toast.makeText(this, "No available quantity for this farmer.", Toast.LENGTH_LONG).show()
        }

        llFarmerDetails.visibility = View.VISIBLE
    }

    // API 3
    private fun sendCurrentSMS(quantity: Double) {
        val d = farmerDetails ?: return
        if (d.farmerId.isNullOrBlank()) return

        val request = SendFarmerSmsRequest(
            farmerId = d.farmerId.orEmpty(),
            districtId = d.districtId ?: districtIdFromIntent,
            farmerName = d.farmerName.orEmpty(),
            fatherHusName = d.fatherHusName.orEmpty(),
            mobileno = d.mobileno.orEmpty(),
            mspCenterPlace = d.mspCenterPlace.orEmpty(),
            mspCenterPlaceName = d.mspCenterPlace.orEmpty(),
            accountNoForSms = d.farmerBankAccountNo.orEmpty(),
            estQty = quantity
        )

        setSmsLoading(true)

        smsCall = RetrofitClient.apiService.sendFarmerSms(request)
        smsCall?.enqueue(object : Callback<SendFarmerSmsResponse> {
            override fun onResponse(call: Call<SendFarmerSmsResponse>, response: Response<SendFarmerSmsResponse>) {
                if (isFinishing || isDestroyed) return
                setSmsLoading(false)

                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    val message = body.message?.takeIf { it.isNotBlank() } ?: "Message has been sent successfully."
                    Toast.makeText(this@SendCurrentSMSActivity, message, Toast.LENGTH_LONG).show()
                    finish() // back to the previous page (MSP dashboard)
                } else {
                    // Stay on this page, details remain filled
                    val message = body?.message?.takeIf { it.isNotBlank() }
                        ?: parseMessage(response.errorBody()?.string())
                        ?: "Unable to send SMS. Please try again."
                    Toast.makeText(this@SendCurrentSMSActivity, message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<SendFarmerSmsResponse>, t: Throwable) {
                if (isFinishing || isDestroyed || call.isCanceled) return
                setSmsLoading(false)
                Toast.makeText(this@SendCurrentSMSActivity, networkMessage(t), Toast.LENGTH_LONG).show()
            }
        })
    }

    // helpers
    private fun setSearchLoading(loading: Boolean) {
        progressSearchFarmer.visibility = if (loading) View.VISIBLE else View.GONE
        btnSearchFarmer.isEnabled = !loading
        edtFarmerId.isEnabled = !loading
    }

    private fun setSmsLoading(loading: Boolean) {
        progressSendCurrentSMS.visibility = if (loading) View.VISIBLE else View.GONE
        btnSendCurrentSMS.isEnabled = !loading
        btnSendCurrentSMS.alpha = if (loading) 0.65f else 1f
        btnSendCurrentSMS.text = if (loading) "Sending..." else "Send SMS"
        edtQuantityToBeReceived.isEnabled = !loading
        btnSearchFarmer.isEnabled = !loading
    }

    private fun clean(value: String?): String {
        val v = value?.trim()
        return if (v.isNullOrEmpty() || v.equals("null", ignoreCase = true)) "—" else v
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.2f", value)

    private fun parseMessage(errorJson: String?): String? = try {
        if (errorJson.isNullOrBlank()) null
        else Gson().fromJson(errorJson, SendFarmerSmsResponse::class.java)?.message?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun networkMessage(t: Throwable): String = when (t) {
        is IOException -> "Unable to connect to the server. Check your internet connection."
        else -> t.localizedMessage ?: "Something went wrong."
    }

    private fun hideKeyboard() {
        currentFocus?.let {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onDestroy() {
        eligibilityCall?.cancel()
        searchCall?.cancel()
        smsCall?.cancel()
        super.onDestroy()
    }
}
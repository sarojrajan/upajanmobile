package com.example.farmerapplication.farmer

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.FarmerLandDetail
import com.example.farmerapplication.models.FarmerLandDetailsResponse
import com.example.farmerapplication.models.FarmerProfileDetailsResponse
import com.example.farmerapplication.models.MSPCenterDetailsResponse
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigDecimal

class FarmerProfileActivity : ComponentActivity() {

    // ================= FARMER PROFILE VIEWS =================
    private lateinit var txtFarmerName: TextView
    private lateinit var txtFarmerId: TextView
    private lateinit var txtDistrict: TextView
    private lateinit var txtMspCentre: TextView
    private lateinit var txtMobile: TextView
    private lateinit var txtAadhar: TextView

    // ================= BASIC DETAILS VIEWS =================
    private lateinit var layoutBasicDetails: LinearLayout
    private lateinit var txtFatherHusbandName: TextView
    private lateinit var txtSubdistrict: TextView
    private lateinit var txtPanchayat: TextView
    private lateinit var txtVillage: TextView
    private lateinit var txtCategory: TextView
    private lateinit var txtBank: TextView
    private lateinit var txtBranch: TextView
    private lateinit var txtAccountNumber: TextView
    private lateinit var txtIFSCCode: TextView

    // ================= LAND DETAILS VIEWS =================
    private lateinit var layoutLandDetails: LinearLayout
    private lateinit var landDetailsContainer: LinearLayout

    // ================= FARMER DATA =================
    private var farmerName: String = ""
    private var farmerId: String = ""
    private var districtName: String = ""
    private var mspCenterName: String = ""
    private var mobileNumber: String = ""
    private var aadharNumber: String = ""

    // ================= DETAIL FLAGS =================
    private var basicDetail: Int = 0
    private var landDetail: Int = 0

    // ================= API CALLS =================
    private var landDetailsApiCall: Call<FarmerLandDetailsResponse>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.farmer_profile)

        initializeViews()
        loadFarmerDetails()
        displayFarmerDetails()
        loadMSPCenterName()
        handleBasicDetails()
        handleLandDetails()
        setupBackButtonHandling()
    }

    /** Initializes all profile, basic details and land details views. */
    private fun initializeViews() {
        txtFarmerName = findViewById(R.id.txtFarmerName)
        txtFarmerId = findViewById(R.id.txtFarmerId)
        txtDistrict = findViewById(R.id.txtDistrict)
        txtMspCentre = findViewById(R.id.txtMspCentre)
        txtMobile = findViewById(R.id.txtMobile)
        txtAadhar = findViewById(R.id.txtAadhar)

        layoutBasicDetails = findViewById(R.id.layoutBasicDetails)
        txtFatherHusbandName = findViewById(R.id.txtFatherHusbandName)
        txtSubdistrict = findViewById(R.id.txtSubdistrict)
        txtPanchayat = findViewById(R.id.txtPanchayat)
        txtVillage = findViewById(R.id.txtVillage)
        txtCategory = findViewById(R.id.txtCategory)
        txtBank = findViewById(R.id.txtBank)
        txtBranch = findViewById(R.id.txtBranch)
        txtAccountNumber = findViewById(R.id.txtAccountNumber)
        txtIFSCCode = findViewById(R.id.txtIFSCCode)

        layoutLandDetails = findViewById(R.id.layoutLandDetails)
        landDetailsContainer = findViewById(R.id.landDetailsContainer)
    }

    /** Loads farmer information and detail flags passed from FarmerMainDashboardActivity. */
    private fun loadFarmerDetails() {
        farmerName = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_FARMER_NAME).orEmpty()
        farmerId = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_FARMER_ID).orEmpty()
        districtName = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_DISTRICT_NAME).orEmpty()
        mspCenterName = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_MSP_CENTER_NAME).orEmpty()
        mobileNumber = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_MOBILE_NUMBER).orEmpty()
        aadharNumber = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_AADHAAR_NUMBER).orEmpty()

        basicDetail = if (intent.hasExtra(FarmerMainDashboardActivity.EXTRA_UPDATED_BASIC_DETAIL)) {
            intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_UPDATED_BASIC_DETAIL, 0)
        } else {
            intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_BASIC_DETAIL, 0)
        }

        landDetail = if (intent.hasExtra(FarmerMainDashboardActivity.EXTRA_UPDATED_LAND_DETAIL)) {
            intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_UPDATED_LAND_DETAIL, 0)
        } else {
            intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_LAND_DETAIL, 0)
        }
    }

    /** Displays Farmer Profile information. */
    private fun displayFarmerDetails() {
        txtFarmerName.text = farmerName.valueOrNotAvailable()
        txtFarmerId.text = farmerId.valueOrNotAvailable()
        txtDistrict.text = districtName.valueOrNotAvailable()
        txtMspCentre.text = if (mspCenterName.isBlank()) "Not Assigned Yet" else mspCenterName
        txtMobile.text = mobileNumber.valueOrNotAvailable()
        txtAadhar.text = formatAadhaarNumber(aadharNumber)
    }

    // ================= BASIC DETAILS =================

    /** Basic Details card is visible only when basicDetail == 1. */
    private fun handleBasicDetails() {
        if (basicDetail == 1) {
            layoutBasicDetails.visibility = View.VISIBLE
            loadBasicDetailsFromApi()
        } else {
            layoutBasicDetails.visibility = View.GONE
        }
    }

    /** GET api/Farmer/GetFarmerProfileDetails?farmerId=... */
    private fun loadBasicDetailsFromApi() {
        if (farmerId.isBlank()) {
            layoutBasicDetails.visibility = View.GONE
            Toast.makeText(this, "Farmer ID is not available.", Toast.LENGTH_SHORT).show()
            return
        }

        showBasicDetailsLoading()

        RetrofitClient.apiService.getFarmerProfileDetails(farmerId).enqueue(object : Callback<FarmerProfileDetailsResponse> {
            override fun onResponse(call: Call<FarmerProfileDetailsResponse>, response: Response<FarmerProfileDetailsResponse>) {
                if (isFinishing || isDestroyed) return

                if (!response.isSuccessful) {
                    showBasicDetailsUnavailable()
                    Toast.makeText(this@FarmerProfileActivity, "Unable to load Basic Details.", Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                if (body?.statusCode != 200 || body.data == null) {
                    showBasicDetailsUnavailable()
                    Toast.makeText(this@FarmerProfileActivity, body?.message ?: "Basic Details not available.", Toast.LENGTH_SHORT).show()
                    return
                }

                val data = body.data
                txtFatherHusbandName.text = data.fatherHusName.orEmpty().valueOrNotAvailable()
                txtSubdistrict.text = data.subdistrictName.orEmpty().valueOrNotAvailable()
                txtPanchayat.text = data.panchayat.orEmpty().valueOrNotAvailable()
                txtVillage.text = data.villageName.orEmpty().valueOrNotAvailable()
                txtCategory.text = getCategoryName(data.category)
                txtBank.text = data.bankName.orEmpty().valueOrNotAvailable()
                txtBranch.text = data.branch.orEmpty().valueOrNotAvailable()
                txtAccountNumber.text = data.accountNumber.orEmpty().valueOrNotAvailable()
                txtIFSCCode.text = data.ifscCode.orEmpty().valueOrNotAvailable()
            }

            override fun onFailure(call: Call<FarmerProfileDetailsResponse>, t: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return
                showBasicDetailsUnavailable()
                Toast.makeText(this@FarmerProfileActivity, "Unable to connect to server.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Temporary values while Basic Details API is loading. */
    private fun showBasicDetailsLoading() {
        txtFatherHusbandName.text = "Loading..."
        txtSubdistrict.text = "Loading..."
        txtPanchayat.text = "Loading..."
        txtVillage.text = "Loading..."
        txtCategory.text = "Loading..."
        txtBank.text = "Loading..."
        txtBranch.text = "Loading..."
        txtAccountNumber.text = "Loading..."
        txtIFSCCode.text = "Loading..."
    }

    /** Used when Basic Details cannot be loaded. */
    private fun showBasicDetailsUnavailable() {
        txtFatherHusbandName.text = "Not Available"
        txtSubdistrict.text = "Not Available"
        txtPanchayat.text = "Not Available"
        txtVillage.text = "Not Available"
        txtCategory.text = "Not Available"
        txtBank.text = "Not Available"
        txtBranch.text = "Not Available"
        txtAccountNumber.text = "Not Available"
        txtIFSCCode.text = "Not Available"
    }

    /** Converts category DB value into readable text. */
    private fun getCategoryName(category: String?): String {
        return when (category?.trim()) {
            "1" -> "General"
            "2" -> "OBC"
            "3" -> "SC"
            "4" -> "ST"
            null, "", "null" -> "Not Available"
            else -> category
        }
    }

    // ================= LAND DETAILS =================

    /** Land Details section is shown ONLY when landDetail == 1. */
    private fun handleLandDetails() {
        if (landDetail == 1) {
            layoutLandDetails.visibility = View.VISIBLE
            loadLandDetailsFromApi()
        } else {
            layoutLandDetails.visibility = View.GONE
            landDetailsContainer.removeAllViews()
        }
    }

    /** GET api/Farmer/GetFarmerLandDetails?farmer_id=... */
    private fun loadLandDetailsFromApi() {
        if (farmerId.isBlank()) {
            showLandDetailsMessage("Farmer ID is not available.")
            return
        }

        showLandDetailsLoading()
        landDetailsApiCall = RetrofitClient.apiService.getFarmerLandDetails(farmerId)
        landDetailsApiCall?.enqueue(object : Callback<FarmerLandDetailsResponse> {
            override fun onResponse(call: Call<FarmerLandDetailsResponse>, response: Response<FarmerLandDetailsResponse>) {
                if (isFinishing || isDestroyed) return

                if (response.code() == 404) {
                    showLandDetailsMessage(getErrorMessage(response, "No land details found for the given Farmer ID."))
                    return
                }

                if (!response.isSuccessful) {
                    val message = getErrorMessage(response, "Unable to load Land Details.")
                    showLandDetailsMessage(message)
                    Toast.makeText(this@FarmerProfileActivity, message, Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                if (body?.statusCode != 200) {
                    showLandDetailsMessage(body?.message?.takeIf { it.isNotBlank() } ?: "Unable to load Land Details.")
                    return
                }

                val landList = body.data
                if (landList.isNullOrEmpty()) {
                    showLandDetailsMessage("No land details found for the given Farmer ID.")
                    return
                }

                displayLandDetails(landList)
            }

            override fun onFailure(call: Call<FarmerLandDetailsResponse>, t: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return
                showLandDetailsMessage("Unable to connect to server.")
                Toast.makeText(this@FarmerProfileActivity, "Unable to connect to server.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Shows loading text inside Land Details section. */
    private fun showLandDetailsLoading() {
        landDetailsContainer.removeAllViews()
        val loadingText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(25), dp(16), dp(25))
            text = "Loading Land Details..."
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        landDetailsContainer.addView(loadingText)
    }

    /** Shows API error / no data message. */
    private fun showLandDetailsMessage(message: String) {
        landDetailsContainer.removeAllViews()
        val messageText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(25), dp(16), dp(25))
            text = message
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        }
        landDetailsContainer.addView(messageText)
    }

    /** Generates one card for every Land Details record. */
    private fun displayLandDetails(landList: List<FarmerLandDetail>) {
        landDetailsContainer.removeAllViews()
        landList.forEachIndexed { index, landDetail ->
            val card = createLandDetailCard(itemNumber = index + 1, landDetail = landDetail)
            landDetailsContainer.addView(card)
        }
    }

    /** Creates one complete Land Details card. */
    private fun createLandDetailCard(itemNumber: Int, landDetail: FarmerLandDetail): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.login_card_bg)
            elevation = dpFloat(10f)
            setPadding(dp(20), dp(24), dp(20), dp(24))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(5) }
        }

        val heading = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            minHeight = dp(48)
            gravity = Gravity.CENTER
            text = "Land Detail $itemNumber"
            setPadding(dp(48), dp(6), dp(48), dp(6))
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD))
            letterSpacing = 0.02f
            setShadowLayer(3f, 1f, 2f, Color.parseColor("#33000000"))
        }
        card.addView(heading)

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)).apply {
                gravity = Gravity.CENTER
                topMargin = dp(4)
                bottomMargin = dp(22)
            }
            setBackgroundColor(Color.parseColor("#66BB6A"))
        }
        card.addView(divider)

        card.addView(createLandValueBlock("Land Type", landDetail.landType.orEmpty().valueOrNotAvailable()))
        card.addView(createLandValueBlock("Circle", landDetail.circleName.orEmpty().valueOrNotAvailable()))
        card.addView(createLandValueBlock("Halka", landDetail.halkaName.orEmpty().valueOrNotAvailable()))
        card.addView(createLandValueBlock("Mauja", landDetail.maujaName.orEmpty().valueOrNotAvailable()))
        card.addView(createLandValueBlock("Volume No in REG II", landDetail.landOwnerName.orEmpty().valueOrNotAvailable()))
        card.addView(createLandValueBlock("Page No in REG II", landDetail.landOwnerRinPustikaNo.orEmpty().valueOrNotAvailable()))
        card.addView(createLandValueBlock("Plot No.", landDetail.plotNo.orEmpty().valueOrNotAvailable()))
        card.addView(createLandValueBlock("Khata No.", landDetail.khasaraNo.orEmpty().valueOrNotAvailable()))
        card.addView(createLandValueBlock("Crop Type", landDetail.crop.orEmpty().valueOrNotAvailable()))
        card.addView(createLandValueBlock("Rakba (in Acre)", formatLandValue(landDetail.rakba)))
        card.addView(createLandValueBlock("Irrigated Land (in Acre)", formatLandValue(landDetail.rakbaCropSinchit)))
        card.addView(createLandValueBlock("UnIrrigated Land (in Acre)", formatLandValue(landDetail.rakbaCropAsinchit)))

        // --- UPDATED LINES ---
        card.addView(createLandValueBlock("Verified by Karamchari", getVerificationText(landDetail.isKarmachariVerify), valueColor = getVerificationColor(landDetail.isKarmachariVerify)))
        card.addView(createLandValueBlock("Verified by CO", getVerificationText(landDetail.isCOVerify), valueColor = getVerificationColor(landDetail.isCOVerify)))
        card.addView(createLandValueBlock("Verified by DSO", getVerificationText(landDetail.isDSOVerify), addBottomMargin = false, valueColor = getVerificationColor(landDetail.isDSOVerify)))

        return card
    }

    /** Creates one label/value block. */
    private fun createLandValueBlock(label: String, value: String, addBottomMargin: Boolean = true, valueColor: Int = Color.BLACK): LinearLayout {
        val block = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.edittext_bg)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                if (addBottomMargin) bottomMargin = dp(12)
            }
        }

        val labelView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            text = label
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
        }

        val valueView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(5) }
            text = value
            setTextColor(valueColor) // <-- Updated to use the passed color
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextIsSelectable(true)
        }

        block.addView(labelView)
        block.addView(valueView)
        return block
    }

    // Convert verification flag into readable value
    private fun getVerificationText(verificationStatus: Int?): String {
        return when (verificationStatus) {
            1 -> "Yes"
            0 -> "No"
            2 -> "Land Records Rejected"
            else -> "Not Available"
        }
    }

    // Get color based on verification status
    private fun getVerificationColor(verificationStatus: Int?): Int {
        return when (verificationStatus) {
            1 -> Color.parseColor("#006400") // Dark Green
            0 -> Color.RED                   // Red
            2 -> Color.parseColor("#8B0000") // Dark Red
            else -> Color.BLACK              // Black
        }
    }

    /** Formats Double values nicely. */
    private fun formatLandValue(value: Double?): String {
        if (value == null) return "Not Available"
        return try {
            BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
        } catch (e: Exception) {
            value.toString()
        }
    }

    /** Gets "message" from non-2xx API error response. */
    private fun getErrorMessage(response: Response<*>, defaultMessage: String): String {
        return try {
            val errorBody = response.errorBody()?.string().orEmpty()
            if (errorBody.isBlank()) return defaultMessage
            val jsonObject = JSONObject(errorBody)
            jsonObject.optString("message", defaultMessage).takeIf { it.isNotBlank() } ?: defaultMessage
        } catch (e: Exception) {
            defaultMessage
        }
    }

    // ================= MSP CENTER =================

    /** Loads MSP Centre name using MSP Centre ID. */
    private fun loadMSPCenterName() {
        if (mspCenterName.isBlank() || mspCenterName.equals("null", ignoreCase = true)) {
            txtMspCentre.text = "Not Assigned Yet"
            return
        }

        RetrofitClient.apiService.getMSPCenterById(mspCenterName).enqueue(object : Callback<MSPCenterDetailsResponse> {
            override fun onResponse(call: Call<MSPCenterDetailsResponse>, response: Response<MSPCenterDetailsResponse>) {
                if (isFinishing || isDestroyed) return
                txtMspCentre.text = if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.statusCode == 200 && !body.mspCenterName.isNullOrBlank()) body.mspCenterName else mspCenterName
                } else {
                    mspCenterName
                }
            }

            override fun onFailure(call: Call<MSPCenterDetailsResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                txtMspCentre.text = mspCenterName
            }
        })
    }

    // ================= COMMON HELPERS =================

    /** Returns "Not Available" for blank/null String values. */
    private fun String.valueOrNotAvailable(): String = if (isBlank() || equals("null", ignoreCase = true)) "Not Available" else this

    /** Masks Aadhaar before displaying. */
    private fun formatAadhaarNumber(aadhaar: String): String {
        val cleanAadhaar = aadhaar.replace(" ", "").replace("-", "").trim()
        if (cleanAadhaar.isBlank() || cleanAadhaar.equals("null", ignoreCase = true)) return "Not Available"

        if (cleanAadhaar.length == 12 && cleanAadhaar.all { it.isDigit() }) return "XXXX XXXX ${cleanAadhaar.takeLast(4)}"
        if (cleanAadhaar.length == 6 && cleanAadhaar.all { it.isDigit() }) return "XXXX XX${cleanAadhaar.take(2)} ${cleanAadhaar.takeLast(4)}"
        if (cleanAadhaar.length == 4 && cleanAadhaar.all { it.isDigit() }) return "XXXX XXXX $cleanAadhaar"

        return cleanAadhaar
    }

    /** Converts dp to pixel Int. */
    private fun dp(value: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    /** Converts dp to pixel Float. Used for elevation. */
    private fun dpFloat(value: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    /** Back button closes FarmerProfileActivity. */
    private fun setupBackButtonHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    /** Cancel running API request if Activity is destroyed. */
    override fun onDestroy() {
        landDetailsApiCall?.cancel()
        super.onDestroy()
    }
}
package com.example.farmerapplication.farmer

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.farmerapplication.MainDashboardActivity
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.AadhaarOtpResponse
import com.example.farmerapplication.models.AadhaarSendOtpRequest
import com.example.farmerapplication.models.AadhaarVerifyOtpRequest
import com.example.farmerapplication.models.ApiErrorResponse
import com.example.farmerapplication.models.District
import com.example.farmerapplication.models.DistrictResponse
import com.example.farmerapplication.models.FarmerRegistrationRequest
import com.example.farmerapplication.models.FarmerRegistrationResponse
import com.example.farmerapplication.models.RegistrationAvailabilityResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class FarmerFirstRegistrationActivity : ComponentActivity() {

    private lateinit var spDistrict: Spinner
    private lateinit var progressDistrict: ProgressBar

    private lateinit var etName: EditText
    private lateinit var etMobile: EditText
    private lateinit var etAadhaar: EditText
    private lateinit var etPassword: EditText

    private lateinit var btnVerifyAadhaar: Button
    private lateinit var imgAadhaarVerified: ImageView
    private lateinit var progressAadhaar: ProgressBar
    private lateinit var txtAadhaarVerificationStatus: TextView
    private lateinit var txtMobileRegistrationStatus: TextView
    private lateinit var txtAadhaarRegistrationStatus: TextView

    private lateinit var chkConfirm: CheckBox

    private lateinit var btnSubmit: Button
    private lateinit var submitBlocker: View
    private lateinit var progressSubmit: ProgressBar

    /** Stores complete district objects for Spinner display. */
    private val districtList = mutableListOf<District>()
    private var selectedDistrict: District? = null

    private var districtApiCall: Call<DistrictResponse>? = null
    private var registrationApiCall: Call<FarmerRegistrationResponse>? = null

    private var mobileCheckApiCall: Call<RegistrationAvailabilityResponse>? = null
    private var aadhaarCheckApiCall: Call<RegistrationAvailabilityResponse>? = null

    private var aadhaarSendOtpApiCall: Call<AadhaarOtpResponse>? = null
    private var aadhaarVerifyOtpApiCall: Call<AadhaarOtpResponse>? = null

    private var isDistrictLoading = false
    private var isRegistrationLoading = false
    private var isAadhaarOtpSending = false
    private var isAadhaarOtpVerifying = false

    private var isMobileChecking = false
    private var isAadhaarChecking = false

    private var isMobileAlreadyRegistered = false
    private var isAadhaarAlreadyRegistered = false

    private var lastCheckedMobileNumber = ""
    private var lastCheckedAadhaarNumber = ""

    /** Aadhaar verification state variables. */
    private var isAadhaarVerified = false
    private var verifiedAadhaarNumber = ""
    private var aadhaarTransactionId = ""
    private var aadhaarMaskedMobileNumber = ""

    private var aadhaarOtpDialog: AlertDialog? = null

    companion object {
        private const val TAG_DISTRICT = "DistrictAPI"
        private const val TAG_REGISTRATION = "RegistrationAPI"
        private const val TAG_AADHAAR = "AadhaarVerification"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.farmer_first_reg_dashboard)

        initializeViews()
        setupInitialDistrictSpinner()
        setupDistrictSelection()

        setupRegistrationAvailabilityChecks()
        setupAadhaarVerification()

        setupConfirmationCheckbox()
        setupSubmitButton()

        loadDistricts()
    }

    private fun initializeViews() {
        spDistrict = findViewById(R.id.spDistrict)
        progressDistrict = findViewById(R.id.progressDistrict)

        etName = findViewById(R.id.etName)
        etMobile = findViewById(R.id.etMobile)
        etAadhaar = findViewById(R.id.etAadhaar)
        etPassword = findViewById(R.id.etPassword)

        txtMobileRegistrationStatus = findViewById(R.id.txtMobileRegistrationStatus)
        txtAadhaarRegistrationStatus = findViewById(R.id.txtAadhaarRegistrationStatus)

        btnVerifyAadhaar = findViewById(R.id.btnVerifyAadhaar)
        imgAadhaarVerified = findViewById(R.id.imgAadhaarVerified)
        progressAadhaar = findViewById(R.id.progressAadhaar)
        txtAadhaarVerificationStatus = findViewById(R.id.txtAadhaarVerificationStatus)

        chkConfirm = findViewById(R.id.chkConfirm)

        btnSubmit = findViewById(R.id.btnSubmit)
        submitBlocker = findViewById(R.id.submitBlocker)
        progressSubmit = findViewById(R.id.progressSubmit)
    }

    /** Shows the default district hint before API completes. */
    private fun setupInitialDistrictSpinner() {
        districtList.clear()
        selectedDistrict = null
        setDistrictSpinnerItems(listOf("Select District"))
        spDistrict.isEnabled = false
    }

    /** Configures Aadhaar verification and text change resets. */
    private fun setupAadhaarVerification() {
        showAadhaarUnverifiedState(showStatus = false)

        btnVerifyAadhaar.setOnClickListener { validateAndSendAadhaarOtp() }

        etAadhaar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                val currentAadhaar = text?.toString()?.trim().orEmpty()
                if (verifiedAadhaarNumber.isNotEmpty() && currentAadhaar != verifiedAadhaarNumber) {
                    resetAadhaarVerification(showStatus = currentAadhaar.isNotEmpty())
                }
                etAadhaar.error = null
                updateSubmitButtonState()
            }

            override fun afterTextChanged(editable: Editable?) = Unit
        })
    }

    /** Controls Submit button interactions using confirmation checkbox. */
    private fun setupConfirmationCheckbox() {
        updateSubmitButtonState()

        chkConfirm.setOnCheckedChangeListener { _, _ -> updateSubmitButtonState() }

        submitBlocker.setOnClickListener {
            when {
                isRegistrationLoading -> showError("Registration is already being submitted.")
                isMobileChecking -> showError("Please wait while the mobile number is being checked.")
                isAadhaarChecking -> showError("Please wait while the Aadhaar number is being checked.")
                isMobileAlreadyRegistered -> showError("Mobile number is already registered.")
                isAadhaarAlreadyRegistered -> showError("Aadhaar number is already registered.")
                isAadhaarOtpSending -> showError("Aadhaar OTP is currently being generated.")
                isAadhaarOtpVerifying -> showError("Aadhaar OTP verification is currently in progress.")
                !isAadhaarVerified -> showError("Please verify your Aadhaar number before submitting registration.")
                !chkConfirm.isChecked -> showError("Please confirm that the entered information is correct by selecting the checkbox.")
                else -> updateSubmitButtonState()
            }
        }
    }

    /** Enables Submit button only when all validation criteria are met. */
    private fun updateSubmitButtonState() {
        val canSubmit = chkConfirm.isChecked && isAadhaarVerified && !isMobileAlreadyRegistered && !isAadhaarAlreadyRegistered && !isMobileChecking && !isAadhaarChecking && !isRegistrationLoading && !isDistrictLoading && !isAadhaarOtpSending && !isAadhaarOtpVerifying

        btnSubmit.isEnabled = canSubmit
        btnSubmit.alpha = if (canSubmit) 1.0f else 0.55f
        submitBlocker.visibility = if (canSubmit) View.GONE else View.VISIBLE
    }

    /** Fetches districts from the API. */
    private fun loadDistricts() {
        showDistrictLoading(true)

        districtApiCall?.cancel()
        districtApiCall = RetrofitClient.apiService.getDistricts()

        districtApiCall?.enqueue(object : Callback<DistrictResponse> {
            override fun onResponse(call: Call<DistrictResponse>, response: Response<DistrictResponse>) {
                showDistrictLoading(false)
                Log.d(TAG_DISTRICT, "HTTP response code: ${response.code()}")
                if (response.isSuccessful) handleSuccessfulDistrictResponse(response.body())
                else handleDistrictErrorResponse(response)
            }

            override fun onFailure(call: Call<DistrictResponse>, throwable: Throwable) {
                showDistrictLoading(false)
                if (call.isCanceled) return

                districtList.clear()
                selectedDistrict = null
                spDistrict.isEnabled = false

                Log.e(TAG_DISTRICT, "District API request failed", throwable)
                val message = when (throwable) {
                    is IOException -> "Unable to connect to the server. Check your internet connection and verify that the API tunnel is running."
                    else -> throwable.localizedMessage ?: "Something went wrong while loading districts."
                }
                showError(message)
            }
        })
    }

    /** Handles a successful district HTTP response. */
    private fun handleSuccessfulDistrictResponse(apiResponse: DistrictResponse?) {
        if (apiResponse == null) {
            districtList.clear()
            selectedDistrict = null
            spDistrict.isEnabled = false
            showError("The server returned an empty district response.")
            return
        }

        Log.d(TAG_DISTRICT, "API status: ${apiResponse.statusCode}, district count: ${apiResponse.data?.size ?: 0}")

        if (apiResponse.statusCode != 200) {
            districtList.clear()
            selectedDistrict = null
            spDistrict.isEnabled = false
            showError(apiResponse.message ?: "Unable to fetch districts.")
            return
        }

        val receivedDistricts = apiResponse.data.orEmpty()
            .filter { district -> district.districtId != null && !district.districtCode.isNullOrBlank() && !district.districtName.isNullOrBlank() }
            .sortedBy { district -> district.districtName?.trim()?.lowercase() }

        if (receivedDistricts.isEmpty()) {
            districtList.clear()
            selectedDistrict = null
            spDistrict.isEnabled = false
            showError("No districts are currently available.")
            return
        }

        districtList.clear()
        districtList.addAll(receivedDistricts)

        val districtNames = mutableListOf("Select District")
        districtNames.addAll(districtList.mapNotNull { district -> district.districtName?.trim()?.takeIf { it.isNotEmpty() } })

        setDistrictSpinnerItems(districtNames)
        selectedDistrict = null
        spDistrict.setSelection(0, false)
        spDistrict.isEnabled = true
    }

    /** Handles district API HTTP errors. */
    private fun handleDistrictErrorResponse(response: Response<DistrictResponse>) {
        districtList.clear()
        selectedDistrict = null
        spDistrict.isEnabled = false

        val rawErrorBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
        Log.e(TAG_DISTRICT, "District API error: HTTP ${response.code()}, body: $rawErrorBody")

        val apiErrorMessage = parseGeneralApiError(rawErrorBody)
        val finalMessage = apiErrorMessage ?: when (response.code()) {
            400 -> "Invalid district request sent to the server."
            401 -> "You are not authorized to access this service."
            403 -> "Access to the district service was denied."
            404 -> "District API was not found. Check the API URL."
            408 -> "The district request timed out. Please try again."
            429 -> "Too many requests. Please try again later."
            in 500..599 -> "A district server error occurred. Please try again later."
            else -> "Unable to load districts. Error code: ${response.code()}"
        }

        showError(finalMessage)
    }

    /** Assigns the district Spinner adapter. */
    private fun setDistrictSpinnerItems(items: List<String>) {
        fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return super.getView(position, convertView, parent).apply {
                    (this as? TextView)?.apply {
                        setTextColor(if (position == 0) Color.parseColor("#757575") else Color.BLACK)
                        textSize = 16f
                        includeFontPadding = true
                        setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                    }
                }
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return super.getDropDownView(position, convertView, parent).apply {
                    (this as? TextView)?.apply {
                        setTextColor(if (position == 0) Color.parseColor("#757575") else Color.BLACK)
                        textSize = 16f
                        includeFontPadding = true
                        setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                    }
                }
            }

            override fun isEnabled(position: Int): Boolean = position != 0
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spDistrict.adapter = adapter
    }

    private fun setupDistrictSelection() {
        spDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDistrict = if (position > 0) districtList.getOrNull(position - 1) else null
                selectedDistrict?.let { district ->
                    Log.d(TAG_DISTRICT, "Selected district: ID=${district.districtId}, code=${district.districtCode}, name=${district.districtName}")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedDistrict = null
            }
        }
    }

    /** Validates Aadhaar number before generating OTP. */
    private fun validateAndSendAadhaarOtp() {
        if (isAadhaarOtpSending || isAadhaarOtpVerifying || isAadhaarChecking) return

        val aadhaarNumber = etAadhaar.text.toString().trim()

        if (aadhaarNumber.isEmpty()) {
            etAadhaar.error = "Please enter your Aadhaar number."
            etAadhaar.requestFocus()
            return
        }

        if (!aadhaarNumber.matches(Regex("^[0-9]{12}$"))) {
            etAadhaar.error = "Aadhaar number must contain exactly 12 digits."
            etAadhaar.requestFocus()
            return
        }

        if (lastCheckedAadhaarNumber != aadhaarNumber) {
            showError("Please wait while the Aadhaar number is being checked.")
            checkAadhaarRegistration(aadhaarNumber)
            return
        }

        if (isAadhaarAlreadyRegistered) {
            txtAadhaarRegistrationStatus.text = "Aadhaar number is already registered."
            txtAadhaarRegistrationStatus.visibility = View.VISIBLE
            showError("Aadhaar number is already registered.")
            return
        }

        resetAadhaarVerification(showStatus = false)
        verifiedAadhaarNumber = aadhaarNumber
        sendAadhaarOtp(aadhaarNumber)
    }

    /** Calls the Aadhaar Generate OTP API. */
    private fun sendAadhaarOtp(aadhaarNumber: String) {
        showAadhaarOtpSending(true)

        val request = AadhaarSendOtpRequest(aadhaarNumber = aadhaarNumber)

        aadhaarSendOtpApiCall?.cancel()
        aadhaarSendOtpApiCall = RetrofitClient.apiService.sendAadhaarOtp(request)

        aadhaarSendOtpApiCall?.enqueue(object : Callback<AadhaarOtpResponse> {
            override fun onResponse(call: Call<AadhaarOtpResponse>, response: Response<AadhaarOtpResponse>) {
                showAadhaarOtpSending(false)
                Log.d(TAG_AADHAAR, "Aadhaar Send OTP HTTP code: ${response.code()}")
                if (response.isSuccessful) {
                    handleAadhaarSendOtpSuccess(aadhaarNumber = aadhaarNumber, apiResponse = response.body())
                } else {
                    handleAadhaarApiError(httpCode = response.code(), rawErrorBody = getErrorBody(response), defaultMessage = "Unable to generate Aadhaar OTP.")
                }
            }

            override fun onFailure(call: Call<AadhaarOtpResponse>, throwable: Throwable) {
                showAadhaarOtpSending(false)
                if (call.isCanceled) return

                resetAadhaarVerification(showStatus = false)
                Log.e(TAG_AADHAAR, "Aadhaar Send OTP request failed", throwable)

                val message = when (throwable) {
                    is IOException -> "Unable to connect to the Aadhaar service. Check your internet connection and confirm that the API tunnel is running."
                    else -> throwable.localizedMessage ?: "Unable to generate Aadhaar OTP."
                }

                showAadhaarFailure(message)
            }
        })
    }

    /** Handles successful HTTP response from Aadhaar Send OTP API. */
    private fun handleAadhaarSendOtpSuccess(aadhaarNumber: String, apiResponse: AadhaarOtpResponse?) {
        if (apiResponse == null) {
            resetAadhaarVerification(showStatus = false)
            showAadhaarFailure("Aadhaar server is down, Please try again after some time..")
            return
        }

        if (apiResponse.statusCode != 200 || apiResponse.success != true) {
            resetAadhaarVerification(showStatus = false)
            val message = buildAadhaarErrorMessage(apiResponse = apiResponse, defaultMessage = "Aadhaar server is down, Please try again after some time..")
            showAadhaarFailure(message)
            return
        }

        val transactionId = apiResponse.transactionId?.trim().orEmpty()
        if (transactionId.isEmpty()) {
            resetAadhaarVerification(showStatus = false)
            showAadhaarFailure("The Aadhaar service did not return a transaction ID.")
            return
        }

        val currentAadhaar = etAadhaar.text.toString().trim()
        if (currentAadhaar != aadhaarNumber) {
            resetAadhaarVerification(showStatus = true)
            showError("The Aadhaar number was changed. Please request a new OTP.")
            return
        }

        verifiedAadhaarNumber = aadhaarNumber
        aadhaarTransactionId = transactionId
        aadhaarMaskedMobileNumber = extractMaskedMobileNumber(apiResponse.aadhaarApiResponse)

        val message = apiResponse.message?.takeIf { it.isNotBlank() } ?: "OTP was sent successfully."
        showToastOnce(message)
        showAadhaarOtpDialog(aadhaarNumber = aadhaarNumber, maskedMobileNumber = aadhaarMaskedMobileNumber)
    }

    /** Displays the custom Aadhaar OTP dialog. */
    private fun showAadhaarOtpDialog(aadhaarNumber: String, maskedMobileNumber: String) {
        aadhaarOtpDialog?.dismiss()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.aadhaar_otp_dialog, null)
        val txtOtpMessage = dialogView.findViewById<TextView>(R.id.txtOtpMessage)
        val edtOtp = dialogView.findViewById<EditText>(R.id.edtOtp)
        val btnCancelOtp = dialogView.findViewById<Button>(R.id.btnCancelOtp)
        val btnVerifyOtp = dialogView.findViewById<Button>(R.id.btnVerifyOtp)
        val progressOtpVerification = dialogView.findViewById<ProgressBar>(R.id.progressOtpVerification)
        val layoutOtpButtons = dialogView.findViewById<LinearLayout>(R.id.layoutOtpButtons)

        val maskedAadhaar = maskAadhaarNumber(aadhaarNumber)

        txtOtpMessage.text = if (maskedMobileNumber.isNotBlank()) {
            "Enter the 6-digit OTP sent to Aadhaar-linked mobile number $maskedMobileNumber for Aadhaar $maskedAadhaar."
        } else {
            "Enter the 6-digit OTP sent to the mobile number linked with Aadhaar $maskedAadhaar."
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancelOtp.setOnClickListener {
            if (!isAadhaarOtpVerifying) {
                dialog.dismiss()
                showAadhaarPendingState("Aadhaar verification was cancelled. Tap Verify to request a new OTP.")
            }
        }

        btnVerifyOtp.setOnClickListener {
            val otp = edtOtp.text.toString().trim()

            if (!otp.matches(Regex("^[0-9]{6}$"))) {
                edtOtp.error = "Please enter the valid 6-digit OTP."
                edtOtp.requestFocus()
                return@setOnClickListener
            }

            if (aadhaarTransactionId.isBlank()) {
                showError("Aadhaar transaction ID is missing. Please request a new OTP.")
                dialog.dismiss()
                resetAadhaarVerification(showStatus = true)
                return@setOnClickListener
            }

            verifyAadhaarOtp(
                aadhaarNumber = aadhaarNumber, otp = otp, dialog = dialog, edtOtp = edtOtp,
                btnCancelOtp = btnCancelOtp, btnVerifyOtp = btnVerifyOtp,
                progressOtpVerification = progressOtpVerification, layoutOtpButtons = layoutOtpButtons
            )
        }

        dialog.setOnDismissListener { if (aadhaarOtpDialog === dialog) aadhaarOtpDialog = null }

        dialog.setOnShowListener {
            dialog.window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
            edtOtp.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }

        aadhaarOtpDialog = dialog
        dialog.show()
    }

    /** Calls the Aadhaar OTP verification API. */
    private fun verifyAadhaarOtp(
        aadhaarNumber: String, otp: String, dialog: AlertDialog, edtOtp: EditText,
        btnCancelOtp: Button, btnVerifyOtp: Button, progressOtpVerification: ProgressBar, layoutOtpButtons: LinearLayout
    ) {
        if (isAadhaarOtpVerifying) return

        val request = AadhaarVerifyOtpRequest(
            aadhaarNumber = aadhaarNumber,
            otp = otp,
            transactionId = aadhaarTransactionId
        )

        showOtpDialogLoading(
            isLoading = true, edtOtp = edtOtp, btnCancelOtp = btnCancelOtp, btnVerifyOtp = btnVerifyOtp,
            progressOtpVerification = progressOtpVerification, layoutOtpButtons = layoutOtpButtons
        )

        aadhaarVerifyOtpApiCall?.cancel()
        aadhaarVerifyOtpApiCall = RetrofitClient.apiService.verifyAadhaarOtp(request)

        aadhaarVerifyOtpApiCall?.enqueue(object : Callback<AadhaarOtpResponse> {
            override fun onResponse(call: Call<AadhaarOtpResponse>, response: Response<AadhaarOtpResponse>) {
                showOtpDialogLoading(
                    isLoading = false, edtOtp = edtOtp, btnCancelOtp = btnCancelOtp, btnVerifyOtp = btnVerifyOtp,
                    progressOtpVerification = progressOtpVerification, layoutOtpButtons = layoutOtpButtons
                )

                Log.d(TAG_AADHAAR, "Aadhaar Verify OTP HTTP code: ${response.code()}")

                if (response.isSuccessful) {
                    handleAadhaarVerificationResponse(aadhaarNumber = aadhaarNumber, apiResponse = response.body(), dialog = dialog, edtOtp = edtOtp)
                } else {
                    val errorResponse = parseAadhaarErrorResponse(getErrorBody(response))
                    val message = buildAadhaarErrorMessage(apiResponse = errorResponse, defaultMessage = getAadhaarHttpErrorMessage(response.code()))

                    edtOtp.text.clear()
                    edtOtp.error = message
                    edtOtp.requestFocus()
                    showAadhaarFailure(message)
                }
            }

            override fun onFailure(call: Call<AadhaarOtpResponse>, throwable: Throwable) {
                showOtpDialogLoading(
                    isLoading = false, edtOtp = edtOtp, btnCancelOtp = btnCancelOtp, btnVerifyOtp = btnVerifyOtp,
                    progressOtpVerification = progressOtpVerification, layoutOtpButtons = layoutOtpButtons
                )

                if (call.isCanceled) return

                Log.e(TAG_AADHAAR, "Aadhaar Verify OTP request failed", throwable)
                val message = when (throwable) {
                    is IOException -> "Unable to connect to the Aadhaar service. Check your internet connection."
                    else -> throwable.localizedMessage ?: "Aadhaar OTP verification failed."
                }

                edtOtp.error = message
                edtOtp.requestFocus()
                showAadhaarFailure(message)
            }
        })
    }

    /** Handles HTTP 200 response from the Aadhaar verification API. */
    private fun handleAadhaarVerificationResponse(
        aadhaarNumber: String, apiResponse: AadhaarOtpResponse?, dialog: AlertDialog, edtOtp: EditText
    ) {
        if (apiResponse == null) {
            val message = "Aadhaar server is down, Please try again after some time.."
            edtOtp.error = message
            showAadhaarFailure(message)
            return
        }

        val isVerified = apiResponse.statusCode == 200 && apiResponse.success == true && apiResponse.aadhaarVerified == true

        if (!isVerified) {
            val message = buildAadhaarErrorMessage(apiResponse = apiResponse, defaultMessage = "The entered Aadhaar OTP could not be verified.")
            edtOtp.text.clear()
            edtOtp.error = message
            edtOtp.requestFocus()
            showAadhaarFailure(message)
            return
        }

        val currentAadhaar = etAadhaar.text.toString().trim()
        if (currentAadhaar != aadhaarNumber) {
            dialog.dismiss()
            resetAadhaarVerification(showStatus = true)
            showError("The Aadhaar number was changed during verification. Please verify it again.")
            return
        }

        isAadhaarVerified = true
        verifiedAadhaarNumber = aadhaarNumber

        dialog.dismiss()
        showAadhaarVerifiedState()
        showToastOnce("Aadhaar verified successfully.")
        updateSubmitButtonState()
    }

    /** Displays OTP verification progress inside the dialog. */
    private fun showOtpDialogLoading(
        isLoading: Boolean, edtOtp: EditText, btnCancelOtp: Button, btnVerifyOtp: Button,
        progressOtpVerification: ProgressBar, layoutOtpButtons: LinearLayout
    ) {
        isAadhaarOtpVerifying = isLoading
        progressOtpVerification.visibility = if (isLoading) View.VISIBLE else View.GONE
        layoutOtpButtons.alpha = if (isLoading) 0.6f else 1.0f

        edtOtp.isEnabled = !isLoading
        btnCancelOtp.isEnabled = !isLoading
        btnVerifyOtp.isEnabled = !isLoading
        btnVerifyOtp.text = if (isLoading) "Verifying..." else "Verify"

        updateSubmitButtonState()
    }

    /** Shows progress while requesting Aadhaar OTP. */
    private fun showAadhaarOtpSending(isLoading: Boolean) {
        isAadhaarOtpSending = isLoading

        progressAadhaar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) btnVerifyAadhaar.visibility = View.GONE

        imgAadhaarVerified.visibility = if (isAadhaarVerified) View.VISIBLE else View.GONE
        etAadhaar.isEnabled = !isLoading && !isRegistrationLoading && !isAadhaarVerified

        updateAadhaarVerifyButtonState()
        updateSubmitButtonState()
    }

    /** Displays successful Aadhaar verification UI. */
    private fun showAadhaarVerifiedState() {
        isAadhaarVerified = true
        btnVerifyAadhaar.visibility = View.GONE
        progressAadhaar.visibility = View.GONE
        imgAadhaarVerified.visibility = View.VISIBLE

        etAadhaar.isEnabled = false
        etAadhaar.alpha = 1.0f

        txtAadhaarVerificationStatus.visibility = View.VISIBLE
        txtAadhaarVerificationStatus.text = "Aadhaar verified successfully"
        txtAadhaarVerificationStatus.setTextColor(Color.parseColor("#06402B"))
    }

    /** Displays an unverified state. */
    private fun showAadhaarUnverifiedState(showStatus: Boolean) {
        isAadhaarVerified = false

        imgAadhaarVerified.visibility = View.GONE
        progressAadhaar.visibility = View.GONE

        etAadhaar.isEnabled = !isRegistrationLoading && !isAadhaarOtpSending

        if (showStatus) {
            txtAadhaarVerificationStatus.visibility = View.VISIBLE
            txtAadhaarVerificationStatus.text = "Aadhaar verification is required"
            txtAadhaarVerificationStatus.setTextColor(Color.parseColor("#FFCC80"))
        } else {
            txtAadhaarVerificationStatus.visibility = View.GONE
            txtAadhaarVerificationStatus.text = ""
        }

        updateAadhaarVerifyButtonState()
    }

    /** Displays Aadhaar pending or cancelled state. */
    private fun showAadhaarPendingState(message: String) {
        isAadhaarVerified = false

        imgAadhaarVerified.visibility = View.GONE
        progressAadhaar.visibility = View.GONE

        etAadhaar.isEnabled = !isRegistrationLoading && !isAadhaarOtpSending

        txtAadhaarVerificationStatus.visibility = View.VISIBLE
        txtAadhaarVerificationStatus.text = message
        txtAadhaarVerificationStatus.setTextColor(Color.parseColor("#FFCC80"))

        updateAadhaarVerifyButtonState()
        updateSubmitButtonState()
    }

    /** Displays Aadhaar failure in both the UI and Toast. */
    private fun showAadhaarFailure(message: String) {
        isAadhaarVerified = false

        imgAadhaarVerified.visibility = View.GONE

        txtAadhaarVerificationStatus.visibility = View.VISIBLE
        txtAadhaarVerificationStatus.text = message
        txtAadhaarVerificationStatus.setTextColor(Color.parseColor("#FFCDD2"))

        updateAadhaarVerifyButtonState()
        updateSubmitButtonState()

        showError(message)
    }

    /** Clears the complete Aadhaar verification state. */
    private fun resetAadhaarVerification(showStatus: Boolean) {
        isAadhaarVerified = false
        verifiedAadhaarNumber = ""
        aadhaarTransactionId = ""
        aadhaarMaskedMobileNumber = ""

        aadhaarOtpDialog?.dismiss()
        aadhaarOtpDialog = null

        showAadhaarUnverifiedState(showStatus)
        updateSubmitButtonState()
    }

    /** Extracts a masked mobile number from XML API responses. */
    private fun extractMaskedMobileNumber(aadhaarApiResponse: String?): String {
        if (aadhaarApiResponse.isNullOrBlank()) return ""
        val pattern = Regex(pattern = """\*{3,}\d{4}""")
        return pattern.find(aadhaarApiResponse)?.value.orEmpty()
    }

    /** Masks Aadhaar as XXXX XXXX 0352. */
    private fun maskAadhaarNumber(aadhaarNumber: String): String {
        if (aadhaarNumber.length != 12) return aadhaarNumber
        return "XXXX XXXX ${aadhaarNumber.takeLast(4)}"
    }

    private fun setupSubmitButton() {
        btnSubmit.setOnClickListener { submitFarmerRegistration() }
    }

    /** Validates the form and submits registration. */
    private fun submitFarmerRegistration() {
        if (isRegistrationLoading) return

        val district = selectedDistrict
        if (district == null) {
            showError("Please select a district.")
            spDistrict.performClick()
            return
        }

        val districtCode = district.districtCode?.trim().orEmpty()
        if (districtCode.isEmpty()) {
            showError("The selected district does not contain a valid district code.")
            return
        }

        val name = etName.text.toString().trim()
        val mobile = etMobile.text.toString().trim()
        val aadhaar = etAadhaar.text.toString().trim()
        val password = etPassword.text.toString()

        if (name.isEmpty()) {
            etName.error = "Please enter your name."
            etName.requestFocus()
            return
        }

        if (name.length < 2) {
            etName.error = "Please enter a valid name."
            etName.requestFocus()
            return
        }

        if (!name.matches(Regex("^[a-zA-Z .'-]+$"))) {
            etName.error = "Name should contain letters and spaces only."
            etName.requestFocus()
            return
        }

        if (mobile.isEmpty()) {
            etMobile.error = "Please enter your mobile number."
            etMobile.requestFocus()
            return
        }

        if (!mobile.matches(Regex("^[6-9][0-9]{9}$"))) {
            etMobile.error = "Enter a valid 10-digit Indian mobile number."
            etMobile.requestFocus()
            return
        }

        if (isMobileChecking) {
            showError("Please wait while the mobile number is being checked.")
            return
        }

        if (isMobileAlreadyRegistered) {
            txtMobileRegistrationStatus.text = "Mobile number is already registered."
            txtMobileRegistrationStatus.visibility = View.VISIBLE
            etMobile.requestFocus()
            showError("Mobile number is already registered.")
            return
        }

        if (aadhaar.isEmpty()) {
            etAadhaar.error = "Please enter your Aadhaar number."
            etAadhaar.requestFocus()
            return
        }

        if (!aadhaar.matches(Regex("^[0-9]{12}$"))) {
            etAadhaar.error = "Aadhaar number must contain exactly 12 digits."
            etAadhaar.requestFocus()
            return
        }

        if (isAadhaarChecking) {
            showError("Please wait while the Aadhaar number is being checked.")
            return
        }

        if (isAadhaarAlreadyRegistered) {
            txtAadhaarRegistrationStatus.text = "Aadhaar number is already registered."
            txtAadhaarRegistrationStatus.visibility = View.VISIBLE
            showError("Aadhaar number is already registered.")
            return
        }

        if (!isAadhaarVerified || verifiedAadhaarNumber != aadhaar) {
            showError("Please verify the entered Aadhaar number before registration.")
            return
        }

        if (password.isBlank()) {
            etPassword.error = "Please enter a password."
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "Password must contain at least 6 characters."
            etPassword.requestFocus()
            return
        }

        if (!chkConfirm.isChecked) {
            showError("Please confirm that the entered information is correct.")
            return
        }

        val request = FarmerRegistrationRequest(
            districtCode = districtCode, name = name, mobile = mobile,
            aadhaar = aadhaar, password = password
        )

        Log.d(TAG_REGISTRATION, "Submitting farmer registration for district code: $districtCode")
        callRegistrationApi(request)
    }

    /** Calls the self-registration API. */
    private fun callRegistrationApi(request: FarmerRegistrationRequest) {
        showRegistrationLoading(true)

        registrationApiCall?.cancel()
        registrationApiCall = RetrofitClient.apiService.registerFarmer(request)

        registrationApiCall?.enqueue(object : Callback<FarmerRegistrationResponse> {
            override fun onResponse(call: Call<FarmerRegistrationResponse>, response: Response<FarmerRegistrationResponse>) {
                showRegistrationLoading(false)
                Log.d(TAG_REGISTRATION, "Registration HTTP code: ${response.code()}")
                if (response.isSuccessful) handleRegistrationSuccess(response.body())
                else handleRegistrationError(response)
            }

            override fun onFailure(call: Call<FarmerRegistrationResponse>, throwable: Throwable) {
                showRegistrationLoading(false)
                if (call.isCanceled) return

                Log.e(TAG_REGISTRATION, "Registration API request failed", throwable)
                val message = when (throwable) {
                    is IOException -> "Unable to connect to the server. Check your internet connection and verify that the API tunnel is running."
                    else -> throwable.localizedMessage ?: "Something went wrong during registration."
                }

                showError(message)
            }
        })
    }

    /** Handles HTTP 200 registration responses. */
    private fun handleRegistrationSuccess(apiResponse: FarmerRegistrationResponse?) {
        if (apiResponse == null) {
            showError("The server returned an empty registration response.")
            return
        }

        Log.d(TAG_REGISTRATION, "Registration API status: ${apiResponse.statusCode}")

        if (apiResponse.statusCode != 200) {
            showError(apiResponse.message ?: "Farmer registration failed.")
            return
        }

        // =======================================================
        // NEW CODE: Silently update Aadhaar verification status
        // =======================================================
        val registeredAadhaar = etAadhaar.text.toString().trim()
        RetrofitClient.apiService.updateAadhaarVerification(registeredAadhaar)
            .enqueue(object : Callback<okhttp3.ResponseBody> {
                override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                }
                override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                }
            })

        val successMessage = apiResponse.message ?: "Registration completed successfully."
        val handler = Handler(Looper.getMainLooper())

        repeat(2) { index ->
            handler.postDelayed({ Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show() }, index * 2500L)
        }

        clearRegistrationForm()

        handler.postDelayed({
            val intent =
                Intent(this@FarmerFirstRegistrationActivity, MainDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 3000L)
    }

    /** Handles HTTP registration errors. */
    private fun handleRegistrationError(response: Response<FarmerRegistrationResponse>) {
        val rawErrorBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
        Log.e(TAG_REGISTRATION, "Registration error: HTTP ${response.code()}, body: $rawErrorBody")

        val registrationError = try {
            if (rawErrorBody.isNullOrBlank()) null
            else Gson().fromJson(rawErrorBody, FarmerRegistrationResponse::class.java)
        } catch (exception: Exception) {
            Log.e(TAG_REGISTRATION, "Unable to parse registration error", exception)
            null
        }

        val apiMessage = registrationError?.message?.takeIf { it.isNotBlank() }

        val finalMessage = apiMessage ?: when (response.code()) {
            400 -> "Please check the entered registration details."
            401 -> "You are not authorized to perform registration."
            403 -> "Registration access was denied."
            404 -> "Registration API was not found. Check the API URL."
            408 -> "The registration request timed out. Please try again."
            409 -> "This mobile number or Aadhaar number is already registered."
            422 -> "The entered registration details are invalid."
            429 -> "Too many registration requests. Please try again later."
            in 500..599 -> "A server error occurred during registration. Please try again later."
            else -> "Registration failed. Error code: ${response.code()}"
        }

        showError(finalMessage)
    }

    /** Parses the Aadhaar failure response. */
    private fun parseAadhaarErrorResponse(rawErrorBody: String?): AadhaarOtpResponse? {
        return try {
            if (rawErrorBody.isNullOrBlank()) null
            else Gson().fromJson(rawErrorBody, AadhaarOtpResponse::class.java)
        } catch (exception: Exception) {
            Log.e(TAG_AADHAAR, "Unable to parse Aadhaar error response", exception)
            null
        }
    }

    private fun handleAadhaarApiError(httpCode: Int, rawErrorBody: String?, defaultMessage: String) {
        resetAadhaarVerification(showStatus = false)

        val errorResponse = parseAadhaarErrorResponse(rawErrorBody)
        val message = buildAadhaarErrorMessage(
            apiResponse = errorResponse,
            defaultMessage = errorResponse?.message ?: getAadhaarHttpErrorMessage(httpCode = httpCode, defaultMessage = defaultMessage)
        )

        showAadhaarFailure(message)
    }

    private fun buildAadhaarErrorMessage(apiResponse: AadhaarOtpResponse?, defaultMessage: String): String {
        val apiMessage = apiResponse?.message?.trim()?.takeIf { it.isNotEmpty() }
        if (apiMessage != null) return apiMessage

        val errorCode = apiResponse?.aadhaarErrorCode?.trim()?.takeIf { it.isNotEmpty() }

        return when (errorCode) {
            "100", "400" -> "The entered Aadhaar details are invalid."
            "300", "403" -> "The Aadhaar OTP is invalid or has expired. Please enter the latest OTP."
            "330" -> "The Aadhaar OTP has expired. Please request a new OTP."
            "570" -> "The OTP verification attempts have been exceeded. Please request a new OTP."
            "CSC:27" -> "The Aadhaar service is currently unable to reach UIDAI. Please try again later."
            null -> defaultMessage
            else -> "Aadhaar server is down, Please try again after some time.."
        }
    }

    private fun getAadhaarHttpErrorMessage(
        httpCode: Int, defaultMessage: String = "Aadhaar server is down, Please try again after some time.."
    ): String {
        return when (httpCode) {
            400 -> "The Aadhaar service rejected the request. Please check the entered information."
            401 -> "The application is not authorized to access the Aadhaar service."
            403 -> "Access to the Aadhaar service was denied."
            404 -> "The Aadhaar API endpoint was not found. Please check the API URL."
            408 -> "The Aadhaar request timed out. Please try again."
            409 -> "The Aadhaar verification transaction is no longer valid. Please request a new OTP."
            422 -> "The Aadhaar request contains invalid information."
            429 -> "Too many Aadhaar verification attempts. Please try again later."
            in 500..599 -> "The Aadhaar service is currently unavailable. Please try again later."
            else -> "$defaultMessage Error code: $httpCode"
        }
    }

    /** Automatically checks whether mobile number or Aadhaar number is registered. */
    private fun setupRegistrationAvailabilityChecks() {
        txtMobileRegistrationStatus.visibility = View.GONE
        txtAadhaarRegistrationStatus.visibility = View.GONE

        isMobileAlreadyRegistered = false
        isAadhaarAlreadyRegistered = false

        lastCheckedMobileNumber = ""
        lastCheckedAadhaarNumber = ""

        // ================= MOBILE NUMBER CHECK =================

        etMobile.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                val mobileNumber = text?.toString()?.trim().orEmpty()
                etMobile.error = null

                if (mobileNumber != lastCheckedMobileNumber) {
                    isMobileAlreadyRegistered = false
                    txtMobileRegistrationStatus.visibility = View.GONE
                    txtMobileRegistrationStatus.text = ""
                }

                if (mobileNumber.length != 10) {
                    mobileCheckApiCall?.cancel()
                    isMobileChecking = false
                    isMobileAlreadyRegistered = false
                    lastCheckedMobileNumber = ""
                    txtMobileRegistrationStatus.visibility = View.GONE
                    txtMobileRegistrationStatus.text = ""
                    updateSubmitButtonState()
                    return
                }

                if (!mobileNumber.matches(Regex("^[6-9][0-9]{9}$"))) {
                    mobileCheckApiCall?.cancel()
                    isMobileChecking = false
                    isMobileAlreadyRegistered = false
                    lastCheckedMobileNumber = ""
                    txtMobileRegistrationStatus.visibility = View.GONE
                    txtMobileRegistrationStatus.text = ""
                    updateSubmitButtonState()
                    return
                }

                checkMobileRegistration(mobileNumber)
            }

            override fun afterTextChanged(editable: Editable?) = Unit
        })

        // ================= AADHAAR NUMBER CHECK =================

        etAadhaar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                val aadhaarNumber = text?.toString()?.trim().orEmpty()

                if (aadhaarNumber != lastCheckedAadhaarNumber) {
                    isAadhaarAlreadyRegistered = false
                    txtAadhaarRegistrationStatus.visibility = View.GONE
                    txtAadhaarRegistrationStatus.text = ""
                }

                btnVerifyAadhaar.isEnabled = false
                btnVerifyAadhaar.alpha = 0.55f

                if (aadhaarNumber.length != 12) {
                    aadhaarCheckApiCall?.cancel()
                    isAadhaarChecking = false
                    isAadhaarAlreadyRegistered = false
                    lastCheckedAadhaarNumber = ""
                    txtAadhaarRegistrationStatus.visibility = View.GONE
                    txtAadhaarRegistrationStatus.text = ""
                    updateAadhaarVerifyButtonState()
                    updateSubmitButtonState()
                    return
                }

                if (!aadhaarNumber.matches(Regex("^[0-9]{12}$"))) {
                    aadhaarCheckApiCall?.cancel()
                    isAadhaarChecking = false
                    isAadhaarAlreadyRegistered = false
                    lastCheckedAadhaarNumber = ""
                    txtAadhaarRegistrationStatus.visibility = View.GONE
                    txtAadhaarRegistrationStatus.text = ""
                    updateAadhaarVerifyButtonState()
                    updateSubmitButtonState()
                    return
                }

                checkAadhaarRegistration(aadhaarNumber)
            }

            override fun afterTextChanged(editable: Editable?) = Unit
        })
    }

    /** Checks whether the entered mobile number is already registered. */
    private fun checkMobileRegistration(mobileNumber: String) {
        mobileCheckApiCall?.cancel()

        isMobileChecking = true
        isMobileAlreadyRegistered = false
        lastCheckedMobileNumber = mobileNumber

        txtMobileRegistrationStatus.visibility = View.GONE
        txtMobileRegistrationStatus.text = ""

        updateSubmitButtonState()

        mobileCheckApiCall = RetrofitClient.apiService.checkMobileNumber(mobileNumber)

        mobileCheckApiCall?.enqueue(object : Callback<RegistrationAvailabilityResponse> {
            override fun onResponse(call: Call<RegistrationAvailabilityResponse>, response: Response<RegistrationAvailabilityResponse>) {
                if (call.isCanceled) return

                val currentMobile = etMobile.text.toString().trim()
                if (currentMobile != mobileNumber) return

                isMobileChecking = false

                if (!response.isSuccessful) {
                    isMobileAlreadyRegistered = false
                    lastCheckedMobileNumber = ""

                    txtMobileRegistrationStatus.visibility = View.GONE
                    txtMobileRegistrationStatus.text = ""

                    Log.e(TAG_REGISTRATION, "Mobile check failed. HTTP ${response.code()}")
                    updateSubmitButtonState()
                    return
                }

                val apiResponse = response.body()
                if (apiResponse == null) {
                    isMobileAlreadyRegistered = false
                    lastCheckedMobileNumber = ""

                    txtMobileRegistrationStatus.visibility = View.GONE
                    txtMobileRegistrationStatus.text = ""

                    updateSubmitButtonState()
                    return
                }

                val message = apiResponse.message?.trim().orEmpty()

                isMobileAlreadyRegistered = message.equals("Mobile number is already registered.", ignoreCase = true) || message.contains("already registered", ignoreCase = true)

                if (isMobileAlreadyRegistered) {
                    txtMobileRegistrationStatus.text = "Mobile number is already registered."
                    txtMobileRegistrationStatus.setTextColor(Color.parseColor("#8B0000"))
                    txtMobileRegistrationStatus.visibility = View.VISIBLE
                } else {
                    txtMobileRegistrationStatus.visibility = View.GONE
                    txtMobileRegistrationStatus.text = ""
                }

                updateSubmitButtonState()
            }

            override fun onFailure(call: Call<RegistrationAvailabilityResponse>, throwable: Throwable) {
                if (call.isCanceled) return

                val currentMobile = etMobile.text.toString().trim()
                if (currentMobile != mobileNumber) return

                isMobileChecking = false
                isMobileAlreadyRegistered = false
                lastCheckedMobileNumber = ""

                txtMobileRegistrationStatus.visibility = View.GONE
                txtMobileRegistrationStatus.text = ""

                Log.e(TAG_REGISTRATION, "Mobile registration check failed", throwable)
                updateSubmitButtonState()
            }
        })
    }

    /** Checks whether the entered Aadhaar number is already registered. */
    private fun checkAadhaarRegistration(aadhaarNumber: String) {
        aadhaarCheckApiCall?.cancel()

        isAadhaarChecking = true
        isAadhaarAlreadyRegistered = false
        lastCheckedAadhaarNumber = aadhaarNumber

        txtAadhaarRegistrationStatus.visibility = View.GONE
        txtAadhaarRegistrationStatus.text = ""

        updateAadhaarVerifyButtonState()
        updateSubmitButtonState()

        aadhaarCheckApiCall = RetrofitClient.apiService.checkAadhaarNumber(aadhaarNumber)

        aadhaarCheckApiCall?.enqueue(object : Callback<RegistrationAvailabilityResponse> {
            override fun onResponse(call: Call<RegistrationAvailabilityResponse>, response: Response<RegistrationAvailabilityResponse>) {
                if (call.isCanceled) return

                val currentAadhaar = etAadhaar.text.toString().trim()
                if (currentAadhaar != aadhaarNumber) return

                isAadhaarChecking = false

                if (!response.isSuccessful) {
                    isAadhaarAlreadyRegistered = false
                    lastCheckedAadhaarNumber = ""

                    txtAadhaarRegistrationStatus.visibility = View.GONE
                    txtAadhaarRegistrationStatus.text = ""

                    Log.e(TAG_AADHAAR, "Aadhaar registration check failed. HTTP ${response.code()}")

                    updateAadhaarVerifyButtonState()
                    updateSubmitButtonState()
                    return
                }

                val apiResponse = response.body()
                if (apiResponse == null) {
                    isAadhaarAlreadyRegistered = false
                    lastCheckedAadhaarNumber = ""

                    txtAadhaarRegistrationStatus.visibility = View.GONE
                    txtAadhaarRegistrationStatus.text = ""

                    updateAadhaarVerifyButtonState()
                    updateSubmitButtonState()
                    return
                }

                val message = apiResponse.message?.trim().orEmpty()

                isAadhaarAlreadyRegistered = message.equals("Aadhaar number is already registered.", ignoreCase = true) || message.contains("already registered", ignoreCase = true)

                if (isAadhaarAlreadyRegistered) {
                    txtAadhaarRegistrationStatus.text = "Aadhaar number is already registered."
                    txtAadhaarRegistrationStatus.setTextColor(Color.parseColor("#8B0000"))
                    txtAadhaarRegistrationStatus.visibility = View.VISIBLE

                    if (isAadhaarVerified) {
                        resetAadhaarVerification(showStatus = false)
                    }
                } else {
                    txtAadhaarRegistrationStatus.visibility = View.GONE
                    txtAadhaarRegistrationStatus.text = ""
                }

                updateAadhaarVerifyButtonState()
                updateSubmitButtonState()
            }

            override fun onFailure(call: Call<RegistrationAvailabilityResponse>, throwable: Throwable) {
                if (call.isCanceled) return

                val currentAadhaar = etAadhaar.text.toString().trim()
                if (currentAadhaar != aadhaarNumber) return

                isAadhaarChecking = false
                isAadhaarAlreadyRegistered = false
                lastCheckedAadhaarNumber = ""

                txtAadhaarRegistrationStatus.visibility = View.GONE
                txtAadhaarRegistrationStatus.text = ""

                Log.e(TAG_AADHAAR, "Aadhaar registration check failed", throwable)

                updateAadhaarVerifyButtonState()
                updateSubmitButtonState()
            }
        })
    }

    /** Centralized Aadhaar Verify button state management. */
    private fun updateAadhaarVerifyButtonState() {
        val aadhaarNumber = etAadhaar.text.toString().trim()
        val hasValidAadhaar = aadhaarNumber.matches(Regex("^[0-9]{12}$"))

        val duplicateCheckCompletedForCurrentNumber = lastCheckedAadhaarNumber == aadhaarNumber && aadhaarNumber.isNotEmpty() && !isAadhaarChecking

        val canVerify = hasValidAadhaar && duplicateCheckCompletedForCurrentNumber && !isAadhaarAlreadyRegistered && !isAadhaarVerified && !isAadhaarOtpSending && !isAadhaarOtpVerifying && !isRegistrationLoading

        btnVerifyAadhaar.isEnabled = canVerify

        if (isAadhaarVerified) {
            btnVerifyAadhaar.visibility = View.GONE
        } else {
            btnVerifyAadhaar.visibility = View.VISIBLE
            btnVerifyAadhaar.alpha = if (canVerify) 1.0f else 0.55f
        }
    }

    private fun <T> getErrorBody(response: Response<T>): String? {
        return try {
            response.errorBody()?.string()
        } catch (exception: Exception) {
            Log.e(TAG_AADHAAR, "Unable to read API error body", exception)
            null
        }
    }

    /** Parses standard ASP.NET validation error responses. */
    private fun parseGeneralApiError(rawErrorBody: String?): String? {
        return try {
            if (rawErrorBody.isNullOrBlank()) return null

            val apiError = Gson().fromJson(rawErrorBody, ApiErrorResponse::class.java)

            when {
                !apiError.message.isNullOrBlank() -> apiError.message
                !apiError.title.isNullOrBlank() -> apiError.title
                !apiError.errors.isNullOrEmpty() -> apiError.errors.values.flatten().joinToString("\n")
                else -> null
            }
        } catch (exception: Exception) {
            Log.e(TAG_DISTRICT, "Unable to parse API error response", exception)
            null
        }
    }

    /** Shows or hides the district loading indicator. */
    private fun showDistrictLoading(isLoading: Boolean) {
        isDistrictLoading = isLoading
        progressDistrict.visibility = if (isLoading) View.VISIBLE else View.GONE
        spDistrict.isEnabled = !isLoading && districtList.isNotEmpty() && !isRegistrationLoading
        updateSubmitButtonState()
    }

    /** Shows or hides the registration loading indicator. */
    private fun showRegistrationLoading(isLoading: Boolean) {
        isRegistrationLoading = isLoading

        progressSubmit.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSubmit.text = if (isLoading) "" else "Submit"

        spDistrict.isEnabled = !isLoading && !isDistrictLoading && districtList.isNotEmpty()
        etName.isEnabled = !isLoading
        etMobile.isEnabled = !isLoading

        etAadhaar.isEnabled = !isLoading && !isAadhaarVerified && !isAadhaarOtpSending
        etPassword.isEnabled = !isLoading
        chkConfirm.isEnabled = !isLoading

        updateAadhaarVerifyButtonState()
        updateSubmitButtonState()
    }

    /** Clears the form after successful registration. */
    private fun clearRegistrationForm() {
        selectedDistrict = null
        spDistrict.setSelection(0, false)

        mobileCheckApiCall?.cancel()
        aadhaarCheckApiCall?.cancel()

        isMobileChecking = false
        isAadhaarChecking = false

        isMobileAlreadyRegistered = false
        isAadhaarAlreadyRegistered = false

        lastCheckedMobileNumber = ""
        lastCheckedAadhaarNumber = ""

        txtMobileRegistrationStatus.visibility = View.GONE
        txtMobileRegistrationStatus.text = ""

        txtAadhaarRegistrationStatus.visibility = View.GONE
        txtAadhaarRegistrationStatus.text = ""

        etName.text.clear()
        etMobile.text.clear()

        etAadhaar.isEnabled = true
        etAadhaar.text.clear()

        etPassword.text.clear()

        etName.error = null
        etMobile.error = null
        etAadhaar.error = null
        etPassword.error = null

        chkConfirm.isChecked = false

        resetAadhaarVerification(showStatus = false)

        updateAadhaarVerifyButtonState()
        updateSubmitButtonState()
    }

    private fun showToastOnce(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showError(message: String) {
        val handler = Handler(Looper.getMainLooper())

        repeat(2) { index ->
            handler.postDelayed({ Toast.makeText(this, message, Toast.LENGTH_LONG).show() }, index * 2500L)
        }
    }

    override fun onDestroy() {

        districtApiCall?.cancel()
        registrationApiCall?.cancel()

        mobileCheckApiCall?.cancel()
        aadhaarCheckApiCall?.cancel()

        aadhaarSendOtpApiCall?.cancel()
        aadhaarVerifyOtpApiCall?.cancel()

        aadhaarOtpDialog?.dismiss()
        aadhaarOtpDialog = null

        super.onDestroy()
    }
}
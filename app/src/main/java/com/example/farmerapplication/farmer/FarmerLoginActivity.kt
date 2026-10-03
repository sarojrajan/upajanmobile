package com.example.farmerapplication.farmer

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.farmer.GenerateOtpRequest
import com.example.farmerapplication.models.farmer.GenerateOtpResponse
import com.example.farmerapplication.models.farmer.VerifyOtpRequest
import com.example.farmerapplication.models.farmer.VerifyOtpResponse
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class FarmerLoginActivity : ComponentActivity() {

    private lateinit var edtMobile: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtRegister: TextView

    private var generateOtpCall: Call<GenerateOtpResponse>? = null
    private var verifyOtpCall: Call<VerifyOtpResponse>? = null

    private var otpDialog: AlertDialog? = null

    /* OTP popup views. */
    private var txtOtpMessage: TextView? = null
    private var edtOtp: EditText? = null
    private var btnCancelOtp: Button? = null
    private var btnVerifyOtp: Button? = null

    private var verifiedMobileNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.farmer_login_dashboard)
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        edtMobile = findViewById(R.id.editTextPhone)
        btnLogin = findViewById(R.id.btnLogin)
        txtRegister = findViewById(R.id.txtRegister)
    }

    private fun setupClickListeners() {
        txtRegister.setOnClickListener { startActivity(
            Intent(
                this,
                FarmerFirstRegistrationActivity::class.java
            )
        ) }
        btnLogin.setOnClickListener { validateAndGenerateOtp() }
    }

    private fun validateAndGenerateOtp() {
        edtMobile.error = null
        val mobile = edtMobile.text?.toString()?.trim().orEmpty()

        when {
            mobile.isBlank() -> { edtMobile.error = "Please enter mobile number"; edtMobile.requestFocus(); return }
            mobile.length != 10 -> { edtMobile.error = "Mobile number must contain 10 digits"; edtMobile.requestFocus(); return }
            !mobile.all { it.isDigit() } -> { edtMobile.error = "Please enter a valid mobile number"; edtMobile.requestFocus(); return }
            mobile.first() !in listOf('6', '7', '8', '9') -> { edtMobile.error = "Please enter a valid Indian mobile number"; edtMobile.requestFocus(); return }
        }

        hideLoginKeyboard()
        callGenerateOtpApi(mobile)
    }

    private fun callGenerateOtpApi(mobile: String) {
        generateOtpCall?.cancel()
        setGenerateOtpLoadingState(true)

        val request = GenerateOtpRequest(mobile = mobile)
        generateOtpCall = RetrofitClient.apiService.generateOtp(request)

        generateOtpCall?.enqueue(object : Callback<GenerateOtpResponse> {
            override fun onResponse(call: Call<GenerateOtpResponse>, response: Response<GenerateOtpResponse>) {
                if (isFinishing || isDestroyed) return
                setGenerateOtpLoadingState(false)
                val responseBody = response.body()

                if (response.isSuccessful && responseBody != null) {
                    if (responseBody.statusCode in 200..299) {
                        verifiedMobileNumber = mobile
                        Toast.makeText(this@FarmerLoginActivity, responseBody.message.ifBlank { "OTP sent successfully" }, Toast.LENGTH_SHORT).show()
                        /* Display OTP box on the current activity. */
                        showOtpVerificationDialog(mobile)
                    } else {
                        showErrorMessage(responseBody.message.ifBlank { "Unable to generate OTP" })
                    }
                    return
                }

                val serverError = try { response.errorBody()?.string() } catch (_: Exception) { null }
                val errorMessage = extractErrorMessage(serverError = serverError, responseCode = response.code(), defaultMessage = "Unable to generate OTP")
                showErrorMessage(errorMessage)
            }

            override fun onFailure(call: Call<GenerateOtpResponse>, throwable: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return
                setGenerateOtpLoadingState(false)

                val errorMessage = when (throwable) {
                    is IOException -> "Unable to connect to the server. Check your internet connection"
                    else -> throwable.localizedMessage ?: "Something went wrong while generating OTP"
                }
                showErrorMessage(errorMessage)
            }
        })
    }

    /** Displays verify_otp_dialoge_box.xml as a centered popup. */
    private fun showOtpVerificationDialog(mobile: String) {
        /* Prevent multiple OTP dialogs from opening together. */
        otpDialog?.dismiss()
        otpDialog = null

        val dialogView = LayoutInflater.from(this).inflate(R.layout.verify_otp_dialoge_box, null, false)
        txtOtpMessage = dialogView.findViewById(R.id.txtOtpMessage)
        edtOtp = dialogView.findViewById(R.id.edtOtp)
        btnCancelOtp = dialogView.findViewById(R.id.btnCancelOtp)
        btnVerifyOtp = dialogView.findViewById(R.id.btnVerifyOtp)

        displayMaskedMobileNumber(mobile)

        otpDialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()

        otpDialog?.setOnShowListener {
            configureOtpDialogWindow()
            showOtpKeyboard()
        }

        otpDialog?.setOnCancelListener { clearOtpDialogReferences() }

        otpDialog?.setOnDismissListener {
            removeBackgroundBlur()
            clearOtpDialogReferences()
        }

        btnCancelOtp?.setOnClickListener { verifyOtpCall?.cancel(); otpDialog?.dismiss() }
        btnVerifyOtp?.setOnClickListener { validateAndVerifyOtp() }

        otpDialog?.show()
    }

    private fun configureOtpDialogWindow() {
        val dialogWindow = otpDialog?.window ?: return

        /* Makes the area outside the dialog transparent so that only the card defined in XML is visible. */
        dialogWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        val windowAttributes = dialogWindow.attributes
        windowAttributes.dimAmount = 0.65f
        dialogWindow.attributes = windowAttributes

        /* Android 12/API 31 and above supports real blur behind the dialog window. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialogWindow.attributes = dialogWindow.attributes.apply { blurBehindRadius = 35 }
            window.setBackgroundBlurRadius(25)
        }
    }

    private fun removeBackgroundBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) window.setBackgroundBlurRadius(0)
    }

    private fun displayMaskedMobileNumber(mobile: String) {
        val lastFourDigits = if (mobile.length >= 4) mobile.takeLast(4) else mobile
        txtOtpMessage?.text = "Enter the OTP sent to the mobile number\n******$lastFourDigits"
    }

    private fun validateAndVerifyOtp() {
        val otpInput = edtOtp ?: return
        otpInput.error = null
        val otpText = otpInput.text?.toString()?.trim().orEmpty()

        when {
            otpText.isBlank() -> { otpInput.error = "Please enter OTP"; otpInput.requestFocus(); return }
            otpText.length != 4 -> { otpInput.error = "OTP must be 4 digits"; otpInput.requestFocus(); return }
            !otpText.all { it.isDigit() } -> { otpInput.error = "Please enter a valid OTP"; otpInput.requestFocus(); return }
        }

        val otpValue = otpText.toIntOrNull()
        if (otpValue == null) {
            otpInput.error = "Please enter a valid OTP"
            otpInput.requestFocus()
            return
        }

        if (verifiedMobileNumber.isBlank()) {
            showErrorMessage("Mobile number is missing. Please generate OTP again")
            otpDialog?.dismiss()
            return
        }

        hideOtpKeyboard()
        callVerifyOtpApi(otpValue)
    }

    private fun callVerifyOtpApi(otp: Int) {
        verifyOtpCall?.cancel()
        setVerifyOtpLoadingState(true)

        val request = VerifyOtpRequest(mobile = verifiedMobileNumber, otp = otp)
        verifyOtpCall = RetrofitClient.apiService.verifyOtp(request)

        verifyOtpCall?.enqueue(object : Callback<VerifyOtpResponse> {
            override fun onResponse(call: Call<VerifyOtpResponse>, response: Response<VerifyOtpResponse>) {
                if (isFinishing || isDestroyed) return
                val responseBody = response.body()

                if (response.isSuccessful && responseBody != null) {
                    if (responseBody.statusCode == 200) {
                        handleVerificationSuccess(responseBody)
                    } else {
                        setVerifyOtpLoadingState(false)
                        showErrorMessage(responseBody.message.ifBlank { "OTP verification failed" })
                    }
                    return
                }

                setVerifyOtpLoadingState(false)
                val serverError = try { response.errorBody()?.string() } catch (_: Exception) { null }
                val errorMessage = extractErrorMessage(serverError = serverError, responseCode = response.code(), defaultMessage = "OTP verification failed")
                showErrorMessage(errorMessage)
            }

            override fun onFailure(call: Call<VerifyOtpResponse>, throwable: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return
                setVerifyOtpLoadingState(false)

                val errorMessage = when (throwable) {
                    is IOException -> "Unable to connect to the server. Check your internet connection"
                    else -> throwable.localizedMessage ?: "Something went wrong while verifying OTP"
                }
                showErrorMessage(errorMessage)
            }
        })
    }

    /** Shows a normal system Toast and redirects directly to the farmer dashboard. */
    private fun handleVerificationSuccess(response: VerifyOtpResponse) {
        hideOtpKeyboard()
        otpDialog?.dismiss()
        Toast.makeText(this, response.message.ifBlank { "OTP verified successfully" }, Toast.LENGTH_SHORT).show()
        openFarmerDashboard(response)
    }

    private fun openFarmerDashboard(response: VerifyOtpResponse) {
        val dashboardIntent = Intent(this, FarmerMainDashboardActivity::class.java).apply {
            putExtra(FarmerMainDashboardActivity.EXTRA_FARMER_NAME, response.farmerName)
            putExtra(FarmerMainDashboardActivity.EXTRA_USER_ID, response.userId)
            putExtra(FarmerMainDashboardActivity.EXTRA_FARMER_ID, response.farmerId)
            putExtra(FarmerMainDashboardActivity.EXTRA_DISTRICT_ID, response.districtId)
            putExtra(FarmerMainDashboardActivity.EXTRA_DISTRICT_NAME, response.districtName)
            putExtra(FarmerMainDashboardActivity.EXTRA_MSP_CENTER_ID, response.mspCenterId)
            putExtra(FarmerMainDashboardActivity.EXTRA_MSP_CENTER_NAME, response.mspCenterName)
            putExtra(FarmerMainDashboardActivity.EXTRA_MOBILE_NUMBER, response.mobileNo)
            /* Send Aadhaar's last six digits received from the OTP verification API. */
            putExtra(FarmerMainDashboardActivity.EXTRA_AADHAAR_NUMBER, response.aadharLast6)
            putExtra(FarmerMainDashboardActivity.EXTRA_BASIC_DETAIL, response.basicDetail)
            putExtra(FarmerMainDashboardActivity.EXTRA_LAND_DETAIL, response.landDetail)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(dashboardIntent)
        finish()
    }

    private fun setGenerateOtpLoadingState(isLoading: Boolean) {
        btnLogin.isEnabled = !isLoading
        edtMobile.isEnabled = !isLoading
        txtRegister.isEnabled = !isLoading
        btnLogin.text = if (isLoading) "Sending OTP..." else "Login"
        btnLogin.alpha = if (isLoading) 0.65f else 1.0f
    }

    private fun setVerifyOtpLoadingState(isLoading: Boolean) {
        btnVerifyOtp?.isEnabled = !isLoading
        btnCancelOtp?.isEnabled = !isLoading
        edtOtp?.isEnabled = !isLoading

        /* Prevent closing the dialog while API verification is in progress. */
        otpDialog?.setCancelable(!isLoading)
        otpDialog?.setCanceledOnTouchOutside(!isLoading)

        btnVerifyOtp?.text = if (isLoading) "Verifying..." else "Verify"
        btnVerifyOtp?.alpha = if (isLoading) 0.65f else 1.0f
        btnCancelOtp?.alpha = if (isLoading) 0.65f else 1.0f
    }

    private fun extractErrorMessage(serverError: String?, responseCode: Int, defaultMessage: String): String {
        if (!serverError.isNullOrBlank()) {
            return try {
                JSONObject(serverError).optString("message", "$defaultMessage. Error code: $responseCode")
            } catch (_: Exception) {
                serverError
            }
        }
        return when (responseCode) {
            400 -> "Invalid request or OTP. Please try again"
            401 -> "You are not authorized to perform this action"
            404 -> "Mobile number was not found"
            500 -> "Server error occurred. Please try again"
            else -> "$defaultMessage. Error code: $responseCode"
        }
    }

    /** System default Toast, used for invalid OTP as well as other API errors. */
    private fun showErrorMessage(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun showOtpKeyboard() {
        val otpInput = edtOtp ?: return
        otpInput.requestFocus()
        otpInput.postDelayed({
            if (otpDialog?.isShowing == true && !isFinishing && !isDestroyed) {
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(otpInput, InputMethodManager.SHOW_IMPLICIT)
            }
        }, 250L)
    }

    private fun hideLoginKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(edtMobile.windowToken, 0)
        edtMobile.clearFocus()
    }

    private fun hideOtpKeyboard() {
        val otpInput = edtOtp ?: return
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(otpInput.windowToken, 0)
        otpInput.clearFocus()
    }

    private fun clearOtpDialogReferences() {
        txtOtpMessage = null
        edtOtp = null
        btnCancelOtp = null
        btnVerifyOtp = null
        otpDialog = null
    }

    override fun onDestroy() {
        generateOtpCall?.cancel(); generateOtpCall = null
        verifyOtpCall?.cancel(); verifyOtpCall = null
        otpDialog?.setOnDismissListener(null)
        otpDialog?.dismiss(); otpDialog = null
        removeBackgroundBlur()
        super.onDestroy()
    }
}
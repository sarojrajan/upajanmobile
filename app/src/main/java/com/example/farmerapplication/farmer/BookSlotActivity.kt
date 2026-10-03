package com.example.farmerapplication.farmer

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.farmer.FarmerSlotBookingRequest
import com.example.farmerapplication.models.farmer.FarmerSlotBookingResponse
import com.example.farmerapplication.models.msp.MSPCenterDetailsResponse
import com.example.farmerapplication.models.farmer.FarmerEligibilityResponse
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.os.Handler
import android.os.Looper
class BookSlotActivity : ComponentActivity() {

    private lateinit var txtSlotMspCenter: TextView
    private lateinit var txtSelectedSlotDate: TextView
    private lateinit var containerSlotDate: LinearLayout
    private lateinit var imgSlotCalendar: ImageView
    private lateinit var btnConfirmBookSlot: Button
    private lateinit var progressBookSlot: ProgressBar

    private var farmerName: String = ""
    private var userId: Int = 0
    private var farmerId: String = ""
    private var districtId: String = ""
    private var districtName: String = ""
    private var mspCenterId: String = ""
    private var mspCenterName: String = ""
    private var mobileNumber: String = ""
    private var aadharNumber: String = ""
    private var basicDetail: Int = 0
    private var landDetail: Int = 0

    private var selectedSlotDate: Date? = null

    private var eligibilityApiCall: Call<FarmerEligibilityResponse>? = null
    private var isEligibilityCheckRunning: Boolean = true

    private val eligibilityHandler = Handler(Looper.getMainLooper())

    private var bookingApiCall: Call<FarmerSlotBookingResponse>? = null
    private var isBookingRequestRunning: Boolean = false

    companion object {
        private const val AVAILABLE_DATE_COUNT = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.slot_booking_layout)

        initializeViews()
        loadFarmerDetails()
        displayFarmerDetails()
        loadMSPCenterName()

        // Check eligibility first.
        checkFarmerEligibility()

        setupClickListeners()
        setupBackButtonHandling()
    }

    private fun initializeViews() {
        txtSlotMspCenter = findViewById(R.id.txtSlotMspCenter)
        txtSelectedSlotDate = findViewById(R.id.txtSelectedSlotDate)
        containerSlotDate = findViewById(R.id.containerSlotDate)
        imgSlotCalendar = findViewById(R.id.imgSlotCalendar)
        btnConfirmBookSlot = findViewById(R.id.btnConfirmBookSlot)
        progressBookSlot = findViewById(R.id.progressBookSlot)
    }

    private fun loadFarmerDetails() {
        farmerName = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_FARMER_NAME).orEmpty().trim()
        userId = intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_USER_ID, 0)
        farmerId = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_FARMER_ID).orEmpty().trim()
        districtId = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_DISTRICT_ID).orEmpty().trim()
        districtName = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_DISTRICT_NAME).orEmpty().trim()
        mspCenterId = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_MSP_CENTER_ID).orEmpty().trim()
        mspCenterName = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_MSP_CENTER_NAME).orEmpty().trim()
        mobileNumber = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_MOBILE_NUMBER).orEmpty().trim()
        aadharNumber = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_AADHAAR_NUMBER).orEmpty().trim()
        basicDetail = intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_BASIC_DETAIL, 0)
        landDetail = intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_LAND_DETAIL, 0)
    }

    private fun displayFarmerDetails() {
        txtSlotMspCenter.text = if (mspCenterName.isNotBlank()) "MSP Center: $mspCenterName" else "MSP Center: Not Assigned Yet"
    }

    private fun setupClickListeners() {
        val dateClickListener = View.OnClickListener {
            if (!isBookingRequestRunning && !isEligibilityCheckRunning) {
                showAvailableDateCalendar()
            }
        }

        containerSlotDate.setOnClickListener(dateClickListener)
        txtSelectedSlotDate.setOnClickListener(dateClickListener)
        imgSlotCalendar.setOnClickListener(dateClickListener)

        btnConfirmBookSlot.setOnClickListener {
            if (!isEligibilityCheckRunning) {
                validateAndBookSlot()
            }
        }
    }

    // Checks whether the farmer is eligible for slot booking
    private fun checkFarmerEligibility() {
        if (farmerId.isBlank() || districtId.isBlank() || mspCenterId.isBlank()) {

            val missingFields = buildString {
                if (farmerId.isBlank()) {
                    append("farmerId is BLANK\n")
                }
                if (districtId.isBlank()) {
                    append("districtId is BLANK\n")
                }
                if (mspCenterId.isBlank()) {
                    append("MSP Centre is not assigned yet\n")
                }
            }

            val debugMessage = """
                Farmer Registration information is missing.
                
                Missing Detail : $missingFields
                """.trimIndent()

            handleEligibilityFailure(debugMessage)
            return
        }

        isEligibilityCheckRunning = true

        eligibilityApiCall = RetrofitClient.apiService.checkFarmerEligibility(farmerId, districtId, mspCenterId)

        eligibilityApiCall?.enqueue(object : Callback<FarmerEligibilityResponse> {
            override fun onResponse(call: Call<FarmerEligibilityResponse>, response: Response<FarmerEligibilityResponse>) {
                if (isFinishing || isDestroyed) return

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody == null) {
                        handleEligibilityFailure("Unable to check farmer eligibility. Please try again.")
                        return
                    }

                    when (responseBody.status) {
                        1 -> isEligibilityCheckRunning = false // Eligible: Remain on current page
                        0 -> {
                            val message = responseBody.message?.takeIf { it.isNotBlank() }
                                ?: "You are Not eligible for Slot booking, Please contact to your DSO Office.."
                            handleEligibilityFailure(message)
                        }
                        else -> handleEligibilityFailure("Unable to verify farmer eligibility. Please try again.")
                    }
                } else {
                    if (response.code() == 409) {
                        val errorBody = response.errorBody()?.string()
                        val message = try {
                            if (!errorBody.isNullOrBlank()) {
                                JSONObject(errorBody).optString("message").takeIf { it.isNotBlank() }
                            } else {
                                null
                            }
                        } catch (_: Exception) {
                            null
                        } ?: "Your Slot is already booked."

                        handleEligibilityFailure(message)
                    } else {
                        handleEligibilityFailure("Unable to check farmer eligibility. Please try again.")
                    }
                }
            }

            override fun onFailure(call: Call<FarmerEligibilityResponse>, throwable: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return
                handleEligibilityFailure("Unable to check farmer eligibility. Please check your internet connection and try again.")
            }
        })
    }

    private fun handleEligibilityFailure(message: String) {
        if (isFinishing || isDestroyed) return

        isEligibilityCheckRunning = true

        // Disable booking controls
        btnConfirmBookSlot.isEnabled = false
        containerSlotDate.isEnabled = false
        imgSlotCalendar.isEnabled = false
        txtSelectedSlotDate.isEnabled = false

        val messageTextView = TextView(this).apply {
            text = message
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(getColor(android.R.color.black))
            minHeight = dpToPx(130)
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }

        val dialog = AlertDialog.Builder(this)
            .setView(messageTextView)
            .setCancelable(false)
            .create()

        dialog.show()

        // Automatically dismiss and finish activity after 5 seconds
        eligibilityHandler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                if (dialog.isShowing) dialog.dismiss()
                finish()
            }
        }, 5000L)
    }

    /**
     * Opens a calendar in which only these three dates are enabled:
     * 1. Today
     * 2. Tomorrow
     * 3. Day after tomorrow
     */
    private fun showAvailableDateCalendar() {
        if (isFinishing || isDestroyed) return

        val todayCalendar = getStartOfDayCalendar()

        // FIX: Removed "Calendar." prefix from add() and set()
        val maximumCalendar = getStartOfDayCalendar().apply {
            add(Calendar.DAY_OF_MONTH, AVAILABLE_DATE_COUNT - 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // FIX: Changed "Calendar.setTime =" to "time ="
        val initiallySelectedCalendar = getStartOfDayCalendar().apply {
            selectedSlotDate?.let { selectedDate -> time = selectedDate }
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (!isDateWithinAvailableRange(selectedCalendar)) {
                    Toast.makeText(
                        this,
                        "Please select today, tomorrow or the day after tomorrow.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@DatePickerDialog
                }

                selectedSlotDate = selectedCalendar.time
                displaySelectedDate(selectedCalendar.time)
            },
            initiallySelectedCalendar.get(Calendar.YEAR),
            initiallySelectedCalendar.get(Calendar.MONTH),
            initiallySelectedCalendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.setTitle("Select Slot Date")
        datePickerDialog.datePicker.minDate = todayCalendar.timeInMillis
        datePickerDialog.datePicker.maxDate = maximumCalendar.timeInMillis

        datePickerDialog.setOnShowListener {
            datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(getColor(android.R.color.holo_green_dark))
            datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(android.R.color.darker_gray))
        }

        datePickerDialog.show()
    }

    private fun isDateWithinAvailableRange(selectedCalendar: Calendar): Boolean {
        val minimumCalendar = getStartOfDayCalendar()

        // FIX: Removed "Calendar." prefix from add()
        val maximumCalendar = getStartOfDayCalendar().apply {
            add(Calendar.DAY_OF_MONTH, AVAILABLE_DATE_COUNT - 1)
        }

        return !selectedCalendar.before(minimumCalendar) && !selectedCalendar.after(maximumCalendar)
    }

    private fun displaySelectedDate(selectedDate: Date) {
        val displayDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        txtSelectedSlotDate.text = displayDateFormat.format(selectedDate)
        txtSelectedSlotDate.setTextColor(getColor(android.R.color.black))
    }

    private fun validateAndBookSlot() {
        if (isBookingRequestRunning) return

        val districtIdValue = districtId.toIntOrNull()

        when {
            districtId.isBlank() -> showValidationDialog("District information is missing. Please log in again.")
            districtIdValue == null || districtIdValue <= 0 -> showValidationDialog("The district ID received from the previous page is invalid.")
            farmerId.isBlank() -> showValidationDialog("Farmer ID is missing. Please log in again.")
            farmerName.isBlank() -> showValidationDialog("Farmer name is missing.")
            mobileNumber.isBlank() -> showValidationDialog("Mobile number is missing.")
            !isValidMobileNumber(mobileNumber) -> showValidationDialog("The farmer mobile number is invalid.")
            mspCenterId.isBlank() -> showValidationDialog("MSP Center is not assigned. Slot booking cannot continue.")
            selectedSlotDate == null -> showValidationDialog("Please select a slot date before booking.")
            getSelectedDateForApi().isBlank() -> showValidationDialog("The selected slot date is invalid. Please select it again.")
            !isSelectedDateStillAvailable() -> {
                selectedSlotDate = null
                txtSelectedSlotDate.text = "Please select a date"
                txtSelectedSlotDate.setTextColor(getColor(android.R.color.darker_gray))
                showValidationDialog("The selected date is no longer available. Please select a new date.")
            }
            else -> submitSlotBooking(districtIdValue = districtIdValue, scheduleDate = getSelectedDateForApi())
        }
    }

    private fun submitSlotBooking(districtIdValue: Int, scheduleDate: String) {
        val request = FarmerSlotBookingRequest(
            districtId = districtIdValue,
            farmerId = farmerId,
            farmerName = farmerName,
            mobileNo = mobileNumber,
            mspCenterPlaceName = mspCenterId,
            scheduleDate = scheduleDate
        )

        setLoadingState(true)

        bookingApiCall = RetrofitClient.apiService.saveFarmerSlotBooking(request)
        bookingApiCall?.enqueue(object : Callback<FarmerSlotBookingResponse> {

            override fun onResponse(call: Call<FarmerSlotBookingResponse>, response: Response<FarmerSlotBookingResponse>) {
                if (isFinishing || isDestroyed) return
                setLoadingState(false)

                if (response.isSuccessful) {
                    handleSuccessfulResponse(response)
                } else {
                    handleUnsuccessfulResponse(response)
                }
            }

            override fun onFailure(call: Call<FarmerSlotBookingResponse>, throwable: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return
                setLoadingState(false)

                val errorMessage = when {
                    throwable.message.isNullOrBlank() -> "Unable to connect to the server. Please check your internet connection."
                    else -> "Unable to book the slot.\n\n${throwable.message}"
                }

                showApiResultDialog(title = "Connection Error", message = errorMessage)
            }
        })
    }

    private fun handleSuccessfulResponse(response: Response<FarmerSlotBookingResponse>) {
        val responseBody = response.body() ?: run {
            showApiResultDialog(title = "Invalid Response", message = "The server returned an empty response.")
            return
        }

        val statusCode = responseBody.statusCode ?: response.code()
        val message = responseBody.message?.takeIf { it.isNotBlank() } ?: "Booking request completed successfully."

        val formattedMessage = buildBookingResponseMessage(
            mainMessage = message,
            scheduleDate = responseBody.scheduleDate,
            estimatedQuantity = responseBody.estimatedQuantity
        )

        val dialogTitle = when (statusCode) {
            200 -> "Slot Booked Successfully"
            409 -> "Slot Already Booked"
            else -> "Booking Response"
        }

        showApiResultDialog(title = dialogTitle, message = formattedMessage)
    }

    private fun handleUnsuccessfulResponse(response: Response<FarmerSlotBookingResponse>) {
        val errorResponse = parseErrorResponse(response.errorBody()?.string())
        val statusCode = errorResponse?.statusCode ?: response.code()

        val defaultMessage = when (statusCode) {
            400 -> "Invalid slot-booking details were submitted."
            401 -> "Your session is not authorized. Please log in again."
            403 -> "You are not allowed to perform this operation."
            404 -> "Farmer or MSP Center details were not found."
            409 -> "Your slot is already booked."
            408 -> "The request timed out. Please try again."
            429 -> "Too many requests were made. Please try again later."
            500 -> "A server error occurred while booking the slot."
            502 -> "The server gateway returned an invalid response."
            503 -> "The booking service is temporarily unavailable."
            504 -> "The server took too long to respond."
            else -> "Unable to complete the slot booking request."
        }

        val message = errorResponse?.message?.takeIf { it.isNotBlank() } ?: defaultMessage

        val formattedMessage = buildBookingResponseMessage(
            mainMessage = message,
            scheduleDate = errorResponse?.scheduleDate,
            estimatedQuantity = errorResponse?.estimatedQuantity
        )

        val dialogTitle = when (statusCode) {
            409 -> "Slot Already Booked"
            400 -> "Invalid Request"
            401, 403 -> "Authorization Error"
            404 -> "Details Not Found"
            in 500..599 -> "Server Error"
            else -> "Booking Failed"
        }

        showApiResultDialog(title = dialogTitle, message = formattedMessage)
    }

    private fun parseErrorResponse(errorBody: String?): FarmerSlotBookingResponse? {
        if (errorBody.isNullOrBlank()) return null

        return try {
            val jsonObject = JSONObject(errorBody)
            FarmerSlotBookingResponse(
                statusCode = if (jsonObject.has("status_code") && !jsonObject.isNull("status_code")) jsonObject.optInt(
                    "status_code"
                ) else null,
                message = jsonObject.optString("message").takeIf { it.isNotBlank() },
                scheduleDate = jsonObject.optString("schedule_date").takeIf { it.isNotBlank() },
                estimatedQuantity = if (jsonObject.has("estimated_quantity") && !jsonObject.isNull("estimated_quantity")) jsonObject.optDouble(
                    "estimated_quantity"
                ) else null
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Loads MSP Centre name using the MSP Centre ID. */
    private fun loadMSPCenterName() {
        if (mspCenterId.isBlank() || mspCenterId.equals("null", ignoreCase = true)) {
            txtSlotMspCenter.text = "MSP Center: Not Assigned Yet"
            return
        }

        RetrofitClient.apiService.getMSPCenterById(mspCenterId).enqueue(object : Callback<MSPCenterDetailsResponse> {
            override fun onResponse(call: Call<MSPCenterDetailsResponse>, response: Response<MSPCenterDetailsResponse>) {
                if (isFinishing || isDestroyed) return

                if (response.isSuccessful) {
                    val responseBody = response.body()

                    if (responseBody?.statusCode == 200 && !responseBody.mspCenterName.isNullOrBlank()) {
                        txtSlotMspCenter.text = "MSP Center: ${responseBody.mspCenterName}"
                    } else {
                        txtSlotMspCenter.text = if (mspCenterName.isNotBlank()) "MSP Center: $mspCenterName" else "MSP Center: $mspCenterId"
                    }
                } else {
                    txtSlotMspCenter.text = if (mspCenterName.isNotBlank()) "MSP Center: $mspCenterName" else "MSP Center: $mspCenterId"
                }
            }

            override fun onFailure(call: Call<MSPCenterDetailsResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                txtSlotMspCenter.text = if (mspCenterName.isNotBlank()) "MSP Center: $mspCenterName" else "MSP Center: $mspCenterId"
            }
        })
    }

    private fun buildBookingResponseMessage(mainMessage: String, scheduleDate: String?, estimatedQuantity: Double?): String {
        return buildString {
            append(mainMessage.trim())
            if (!scheduleDate.isNullOrBlank()) {
                append("\nSchedule Date: ").append(scheduleDate)
            }
            if (estimatedQuantity != null && !estimatedQuantity.isNaN()) {
                append("\nEstimated Quantity: ").append(formatQuantity(estimatedQuantity))
            }
        }
    }

    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            quantity.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", quantity).trimEnd('0').trimEnd('.')
        }
    }

    private fun showValidationDialog(message: String) {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("Required Information")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showApiResultDialog(title: String, message: String) {
        if (isFinishing || isDestroyed) return

        val messageTextView = TextView(this).apply {
            text = message
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(getColor(android.R.color.black))
            setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(12))
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(messageTextView)
            .setCancelable(false)
            .setPositiveButton("OK") { alertDialog, _ ->
                alertDialog.dismiss()
                if (!isFinishing) finish()
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(android.R.color.holo_green_dark))
        }

        dialog.show()
    }

    private fun setLoadingState(isLoading: Boolean) {
        isBookingRequestRunning = isLoading
        progressBookSlot.visibility = if (isLoading) View.VISIBLE else View.GONE

        btnConfirmBookSlot.isEnabled = !isLoading
        containerSlotDate.isEnabled = !isLoading
        imgSlotCalendar.isEnabled = !isLoading
        txtSelectedSlotDate.isEnabled = !isLoading

        btnConfirmBookSlot.alpha = if (isLoading) 0.6f else 1.0f
        containerSlotDate.alpha = if (isLoading) 0.7f else 1.0f
        btnConfirmBookSlot.text = if (isLoading) "Booking Slot..." else "Book Slot"
    }

    private fun isValidMobileNumber(mobile: String): Boolean = mobile.matches(Regex("^[6-9][0-9]{9}$"))

    private fun isSelectedDateStillAvailable(): Boolean {
        val selectedDate = selectedSlotDate ?: return false
        val selectedCalendar = Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return isDateWithinAvailableRange(selectedCalendar)
    }

    private fun getSelectedDateForApi(): String {
        val date = selectedSlotDate ?: return ""
        return SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(date)
    }

    private fun getStartOfDayCalendar(): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun setupBackButtonHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isBookingRequestRunning) {
                    Toast.makeText(this@BookSlotActivity, "Please wait while the slot is being booked.", Toast.LENGTH_SHORT).show()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onDestroy() {

        eligibilityApiCall?.cancel()
        eligibilityApiCall = null

        eligibilityHandler.removeCallbacksAndMessages(null)

        bookingApiCall?.cancel()
        bookingApiCall = null

        super.onDestroy()
    }
}
package com.example.farmerapplication.farmer

import android.app.AlertDialog
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.example.farmerapplication.MainDashboardActivity
import com.example.farmerapplication.R

class FarmerMainDashboardActivity : ComponentActivity() {

    private lateinit var btnLogout: ImageView
    private lateinit var txtWelcomeFarmer: TextView
    private lateinit var btnBookSlot: LinearLayout
    private lateinit var btnViewProfile: LinearLayout
    private lateinit var btnBasicDetails: LinearLayout
    private lateinit var btnLandEntry: LinearLayout

    private var farmerName: String = ""
    private var userId: Int = 0
    private var farmerId: String = ""
    private var districtId: String = ""
    private var districtName: String = ""
    private var mspCenterId: String = ""
    private var mspCenterName: String = ""
    private var mobileNumber: String = ""
    private var aadharNumber: String = ""

    /*
     * These two values are updated immediately after successful
     * completion of Basic Details or Land Details.
     */
    private var basicDetail: Int = 0
    private var landDetail: Int = 0

    /*
     * Used for handling five-second dashboard messages.
     */
    private val messageHandler = Handler(Looper.getMainLooper())
    private var activeToast: Toast? = null

    companion object {
        const val EXTRA_FARMER_NAME = "extra_farmer_name"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_FARMER_ID = "extra_farmer_id"
        const val EXTRA_DISTRICT_ID = "extra_district_id"
        const val EXTRA_DISTRICT_NAME = "extra_district_name"
        const val EXTRA_MSP_CENTER_ID = "extra_msp_center_id"
        const val EXTRA_MSP_CENTER_NAME = "extra_msp_center_name"
        const val EXTRA_MOBILE_NUMBER = "extra_mobile_number"
        const val EXTRA_AADHAAR_NUMBER = "extra_aadhaar_number"
        const val EXTRA_BASIC_DETAIL = "extra_basic_detail"
        const val EXTRA_LAND_DETAIL = "extra_land_detail"

        /*
         * Result extras returned by FarmerFullRegActivity and
         * LandDetailsEntryActivity.
         */
        const val EXTRA_UPDATED_BASIC_DETAIL = "extra_updated_basic_detail"
        const val EXTRA_UPDATED_LAND_DETAIL = "extra_updated_land_detail"
        const val EXTRA_SUCCESS_MESSAGE = "extra_success_message"
    }

    /*
     * Receives the successful Basic Details submission result.
     */
    private val basicDetailsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        val resultIntent = result.data
        basicDetail = resultIntent?.getIntExtra(EXTRA_UPDATED_BASIC_DETAIL, basicDetail) ?: basicDetail

        /* Keep landDetail synchronized in case it is returned. */
        landDetail = resultIntent?.getIntExtra(EXTRA_UPDATED_LAND_DETAIL, landDetail) ?: landDetail

        updateDashboardBlockStates()

        val successMessage = resultIntent?.getStringExtra(EXTRA_SUCCESS_MESSAGE).orEmpty().ifBlank {
            "Basic Details filled successfully. Please fill the Land Details."
        }

        showFiveSecondMessage(successMessage)
    }

    /*
     * Receives the successful Land Details submission result.
     */
    private val landDetailsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        val resultIntent = result.data
        landDetail = resultIntent?.getIntExtra(EXTRA_UPDATED_LAND_DETAIL, landDetail) ?: landDetail

        /* Keep basicDetail synchronized in case it is returned. */
        basicDetail = resultIntent?.getIntExtra(EXTRA_UPDATED_BASIC_DETAIL, basicDetail) ?: basicDetail

        updateDashboardBlockStates()

        val successMessage = resultIntent?.getStringExtra(EXTRA_SUCCESS_MESSAGE).orEmpty().ifBlank {
            "Land Details filled successfully. You can now book your slot."
        }

        showFiveSecondMessage(successMessage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.farmer_dashboard_main)

        initializeViews()
        loadFarmerDetails()
        displayFarmerName()
        setupClickListeners()
        setupBackButtonHandling()

        /* Apply the initial blur state received from the login response. */
        updateDashboardBlockStates()
    }

    private fun initializeViews() {
        btnLogout = findViewById(R.id.btnLogout)
        txtWelcomeFarmer = findViewById(R.id.txtWelcomeFarmer)
        btnBookSlot = findViewById(R.id.btnBookSlot)
        btnViewProfile = findViewById(R.id.btnViewProfile)
        btnBasicDetails = findViewById(R.id.btnBasicDetails)
        btnLandEntry = findViewById(R.id.btnLandEntry)
    }

    private fun loadFarmerDetails() {
        farmerName = intent.getStringExtra(EXTRA_FARMER_NAME).orEmpty()
        userId = intent.getIntExtra(EXTRA_USER_ID, 0)
        farmerId = intent.getStringExtra(EXTRA_FARMER_ID).orEmpty()
        districtId = intent.getStringExtra(EXTRA_DISTRICT_ID).orEmpty()
        districtName = intent.getStringExtra(EXTRA_DISTRICT_NAME).orEmpty()
        mspCenterId = intent.getStringExtra(EXTRA_MSP_CENTER_ID).orEmpty()
        mspCenterName = intent.getStringExtra(EXTRA_MSP_CENTER_NAME).orEmpty()
        mobileNumber = intent.getStringExtra(EXTRA_MOBILE_NUMBER).orEmpty()
        aadharNumber = intent.getStringExtra(EXTRA_AADHAAR_NUMBER).orEmpty()
        basicDetail = intent.getIntExtra(EXTRA_BASIC_DETAIL, 0)
        landDetail = intent.getIntExtra(EXTRA_LAND_DETAIL, 0)
    }

    private fun displayFarmerName() {
        txtWelcomeFarmer.text = farmerName.ifBlank { "Welcome, Farmer" }
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener { showLogoutConfirmationDialog() }
        btnViewProfile.setOnClickListener { openFarmerProfile() }

        /*
         * BASIC DETAILS RULES:
         * basicDetail = 1: Do not reopen the form. Show an informational message.
         * basicDetail = 0: Open FarmerFullRegActivity.
         */
        btnBasicDetails.setOnClickListener {
            if (basicDetail == 1) {
                showFiveSecondMessage("You have already filled the Basic Details.")
            } else {
                openFarmerFullRegistration()
            }
        }

        /*
         * LAND DETAILS RULES:
         * landDetail = 1: Do not reopen the form.
         * basicDetail = 0: Basic Details must be completed first.
         * basicDetail = 1 and landDetail = 0: Open LandDetailsEntryActivity.
         */
        btnLandEntry.setOnClickListener {
            when {
                landDetail == 1 -> showFiveSecondMessage("You have already filled the Land Details.")
                basicDetail == 0 -> showFiveSecondMessage("Please fill the Basic Details first.")
                else -> openLandEntry()
            }
        }

        /*
         * SLOT BOOKING RULES:
         * basicDetail = 0 and landDetail = 0: Both sections must be completed.
         * basicDetail = 1 and landDetail = 0: Land Details must be completed.
         * basicDetail = 1 and landDetail = 1: Slot Booking is available.
         */
        btnBookSlot.setOnClickListener {
            when {
                basicDetail == 0 && landDetail == 0 -> showFiveSecondMessage("Please fill the Basic Details and Land Details first.")
                basicDetail == 0 -> showFiveSecondMessage("Please fill the Basic Details first.")
                landDetail == 0 -> showFiveSecondMessage("Please fill the Land Details first.")
                else -> openBookSlot()
            }
        }
    }

    /**
     * Updates the blur appearance of all dashboard blocks.
     */
    private fun updateDashboardBlockStates() {
        val basicDetailsCompleted = basicDetail == 1
        val landDetailsCompleted = landDetail == 1
        val slotBookingAvailable = basicDetailsCompleted && landDetailsCompleted

        setBlockBlurred(block = btnBasicDetails, blurred = basicDetailsCompleted)
        setBlockBlurred(block = btnLandEntry, blurred = landDetailsCompleted)
        setBlockBlurred(block = btnBookSlot, blurred = !slotBookingAvailable)
    }

    /**
     * Applies a real blur effect on Android 12/API 31 and above.
     */
    private fun setBlockBlurred(block: LinearLayout, blurred: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (blurred) {
                block.setRenderEffect(RenderEffect.createBlurEffect(5f, 5f, Shader.TileMode.CLAMP))
            } else {
                block.setRenderEffect(null)
            }
        }

        block.alpha = if (blurred) 0.35f else 1.0f

        /* Keep it clickable because completed/locked blocks must display messages. */
        block.isClickable = true
        block.isFocusable = true
    }

    /**
     * Opens BookSlotActivity and passes all farmer information.
     */
    private fun openBookSlot() {
        val bookSlotIntent = Intent(this, BookSlotActivity::class.java).apply { addFarmerExtras(this) }
        startActivity(bookSlotIntent)
    }

    private fun openFarmerProfile() {
        val profileIntent = Intent(this, FarmerProfileActivity::class.java).apply { addFarmerExtras(this) }
        startActivity(profileIntent)
    }

    private fun openFarmerFullRegistration() {
        val registrationIntent = Intent(this, FarmerFullRegActivity::class.java).apply { addFarmerExtras(this) }
        basicDetailsLauncher.launch(registrationIntent)
    }

    private fun openLandEntry() {
        val landIntent = Intent(this, LandDetailsEntryActivity::class.java).apply {
            putExtra(EXTRA_FARMER_ID, farmerId)
            putExtra(EXTRA_DISTRICT_ID, districtId)
            putExtra(EXTRA_BASIC_DETAIL, basicDetail)
            putExtra(EXTRA_LAND_DETAIL, landDetail)
        }
        landDetailsLauncher.launch(landIntent)
    }

    /**
     * Adds the common farmer details to another activity Intent.
     */
    private fun addFarmerExtras(targetIntent: Intent) {
        targetIntent.putExtra(EXTRA_FARMER_NAME, farmerName)
        targetIntent.putExtra(EXTRA_USER_ID, userId)
        targetIntent.putExtra(EXTRA_FARMER_ID, farmerId)
        targetIntent.putExtra(EXTRA_DISTRICT_ID, districtId)
        targetIntent.putExtra(EXTRA_DISTRICT_NAME, districtName)
        targetIntent.putExtra(EXTRA_MSP_CENTER_ID, mspCenterId)
        targetIntent.putExtra(EXTRA_MSP_CENTER_NAME, mspCenterName)
        targetIntent.putExtra(EXTRA_MOBILE_NUMBER, mobileNumber)
        targetIntent.putExtra(EXTRA_AADHAAR_NUMBER, aadharNumber)

        // Original flags
        targetIntent.putExtra(EXTRA_BASIC_DETAIL, basicDetail)
        targetIntent.putExtra(EXTRA_LAND_DETAIL, landDetail)

        // Current/updated flags
        targetIntent.putExtra(EXTRA_UPDATED_BASIC_DETAIL, basicDetail)
        targetIntent.putExtra(EXTRA_UPDATED_LAND_DETAIL, landDetail)
    }

    /**
     * Displays a message for approximately five seconds.
     */
    private fun showFiveSecondMessage(message: String) {
        messageHandler.removeCallbacksAndMessages(null)
        activeToast?.cancel()

        activeToast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        activeToast?.show()

        messageHandler.postDelayed({ activeToast?.show() }, 2_500L)
        messageHandler.postDelayed({
            activeToast?.cancel()
            activeToast = null
        }, 5_000L)
    }

    /**
     * Kept unchanged for future use.
     */
    private fun showFeatureComingSoon() {
        Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun setupBackButtonHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = showLogoutConfirmationDialog()
            }
        )
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setCancelable(true)
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                val mainDashboardIntent = Intent(this, MainDashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(mainDashboardIntent)
                finish()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroy() {
        messageHandler.removeCallbacksAndMessages(null)
        activeToast?.cancel()
        activeToast = null
        super.onDestroy()
    }
}
package com.example.farmerapplication.msp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.example.farmerapplication.MainDashboardActivity
import com.example.farmerapplication.R
import com.example.farmerapplication.models.MSPLoginResponse
import com.google.gson.Gson

class MSPDashboardActivity : ComponentActivity() {
    private lateinit var btnLogout: ImageView
    private lateinit var txtWelcomeMSP: TextView
    private lateinit var txtDashboardHeading: TextView
    private lateinit var btnViewProfile: LinearLayout
    private lateinit var btnMonitorDetails: LinearLayout
    private lateinit var btnCurrentSlotBook: LinearLayout
    private lateinit var btnMSPReport: LinearLayout

    private var mspResponse: MSPLoginResponse? = null

    companion object {
        const val EXTRA_MSP_RESPONSE = "extra_msp_response"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msp_dashboard_main)
        initializeViews()
        receiveLoginDetails()
        displayMSPDetails()
        setupClickListeners()
        setupBackPressHandler()
    }

    private fun initializeViews() {
        btnLogout = findViewById(R.id.btnLogout)
        txtWelcomeMSP = findViewById(R.id.txtWelcomeMSP)
        txtDashboardHeading = findViewById(R.id.txtDashboardHeading)
        btnViewProfile = findViewById(R.id.btnViewProfile)
        btnMonitorDetails = findViewById(R.id.btnMonitorDetails)
        btnCurrentSlotBook = findViewById(R.id.btnCurrentSlotBook)
        btnMSPReport = findViewById(R.id.btnMSPReport)
    }

    private fun receiveLoginDetails() {
        val responseJson = intent.getStringExtra(EXTRA_MSP_RESPONSE)
        if (responseJson.isNullOrBlank()) return

        try {
            mspResponse = Gson().fromJson(responseJson, MSPLoginResponse::class.java)
        } catch (_: Exception) {
            mspResponse = null
        }
    }

    private fun displayMSPDetails() {
        val response = mspResponse
        val displayName = response?.mspCentreName?.trim()?.takeIf { it.isNotBlank() } ?: "MSP User"

        txtWelcomeMSP.text = displayName
        txtDashboardHeading.text = "MSP's Dashboard"

        if (response?.mspCentreId == null) {
            Toast.makeText(this, "MSP login information is unavailable.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        btnViewProfile.setOnClickListener { openMSPProfile() }
        btnMonitorDetails.setOnClickListener { openMonitorDetails() }
        btnLogout.setOnClickListener { showLogoutConfirmation() }
        btnCurrentSlotBook.setOnClickListener { openCurrentSlotBooking() }
        btnMSPReport.setOnClickListener { openMSPMainReports() }
    }

    private fun openCurrentSlotBooking() {
        val response = mspResponse
        val districtId = response?.districtId?.toString()?.trim().orEmpty()
        val mspCentreId = response?.mspCentreId?.toString()?.trim().orEmpty()

        if (districtId.isBlank() || mspCentreId.isBlank()) {
            Toast.makeText(this, "MSP login information is unavailable.", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(this, SendCurrentSMSActivity::class.java).apply {
            putExtra(SendCurrentSMSActivity.EXTRA_DISTRICT_ID, districtId)
            putExtra(SendCurrentSMSActivity.EXTRA_SOC_ID, mspCentreId)
        }
        startActivity(intent)
    }

    private fun openMSPProfile() {
        if (mspResponse == null) {
            Toast.makeText(this, "MSP profile information is unavailable.", Toast.LENGTH_LONG).show()
            return
        }

        val responseJson = Gson().toJson(mspResponse)
        val profileIntent = Intent(this, MSPProfileActivity::class.java).apply {
            putExtra(MSPProfileActivity.EXTRA_MSP_RESPONSE, responseJson)
        }
        startActivity(profileIntent)
    }

    // Opens MonitorDetailsActivity with the MSP Centre ID
    private fun openMonitorDetails() {
        val response = mspResponse
        if (response == null) {
            Toast.makeText(this, "MSP login information is unavailable.", Toast.LENGTH_LONG).show()
            return
        }

        val mspCentreId = response.mspCentreId?.toString()?.trim().orEmpty()
        if (mspCentreId.isBlank()) {
            Toast.makeText(this, "MSP Centre ID is unavailable.", Toast.LENGTH_LONG).show()
            return
        }

        val monitorIntent = Intent(this, MonitorDetailsActivity::class.java).apply {
            putExtra(MonitorDetailsActivity.EXTRA_MSP_CENTRE_ID, mspCentreId)
        }
        startActivity(monitorIntent)
    }

    // OPENS MSP REPORTS WITH MSP CENTRE ID
    private fun openMSPMainReports(){
        val response = mspResponse
        if (response == null) {
            Toast.makeText(this, "MSP login information is unavailable.", Toast.LENGTH_LONG).show()
            return
        }

        val mspCentreId = response.mspCentreId?.toString()?.trim().orEmpty()
        if (mspCentreId.isBlank()) {
            Toast.makeText(this, "MSP Centre ID is unavailable.", Toast.LENGTH_LONG).show()
            return
        }
        val MSPReportIntent = Intent(this, MSPReportMainActivity::class.java).apply {
            putExtra(MSPReportMainActivity.EXTRA_MSP_CENTRE_ID, mspCentreId)
        }
        startActivity(MSPReportIntent)
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setIcon(R.drawable.ic_logout)
            .setNegativeButton("No", null)
            .setPositiveButton("Yes") { _, _ -> logoutMSP() }
            .show()
    }

    private fun logoutMSP() {
        val loginIntent = Intent(this, MainDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
        finish()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Back button performs the same action as Logout
                    showLogoutConfirmation()
                }
            }
        )
    }

    private fun showFeatureComingSoon() {
        Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
    }
}
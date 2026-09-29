package com.example.farmerapplication.miller

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.farmerapplication.R

class MillerProfileActivity : ComponentActivity() {

    private lateinit var txtMillerId: TextView
    private lateinit var txtMillerName: TextView
    private lateinit var txtDistrict: TextView
    private lateinit var txtEmailId: TextView
    private lateinit var txtMobileNumber: TextView
    private lateinit var txtTinNumber: TextView
    private lateinit var txtPan: TextView
    private lateinit var txtLicenseNumber: TextView
    private lateinit var txtBank: TextView
    private lateinit var txtBranch: TextView
    private lateinit var txtAccountNumber: TextView
    private lateinit var txtIfscCode: TextView
    private lateinit var txtMillerHeadName: TextView
    private lateinit var txtMillingCapacity: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.miller_profile_layout)

        initializeViews()
        displayMillerDetails()
    }

    private fun initializeViews() {
        txtMillerId = findViewById(R.id.txtMillerId)
        txtMillerName = findViewById(R.id.txtMillerName)
        txtDistrict = findViewById(R.id.txtDistrict)
        txtEmailId = findViewById(R.id.txtEmailId)
        txtMobileNumber = findViewById(R.id.txtMobileNumber)
        txtTinNumber = findViewById(R.id.txtTinNumber)
        txtPan = findViewById(R.id.txtPan)
        txtLicenseNumber = findViewById(R.id.txtLicenseNumber)
        txtBank = findViewById(R.id.txtBank)
        txtBranch = findViewById(R.id.txtBranch)
        txtAccountNumber = findViewById(R.id.txtAccountNumber)
        txtIfscCode = findViewById(R.id.txtIfscCode)
        txtMillerHeadName = findViewById(R.id.txtMillerHeadName)
        txtMillingCapacity = findViewById(R.id.txtMillingCapacity)
    }

    private fun displayMillerDetails() {
        val millerId = intent.getIntExtra(MillerDashboardActivity.EXTRA_MILLER_ID, 0)
        val millerName = intent.getStringExtra(MillerDashboardActivity.EXTRA_MILLER_NAME)
        val districtName = intent.getStringExtra(MillerDashboardActivity.EXTRA_DISTRICT_NAME)
        val emailId = intent.getStringExtra(MillerDashboardActivity.EXTRA_EMAIL_ID)
        val mobileNo = intent.getStringExtra(MillerDashboardActivity.EXTRA_MOBILE_NO)
        val tinNo = intent.getStringExtra(MillerDashboardActivity.EXTRA_TINNO)
        val pan = intent.getStringExtra(MillerDashboardActivity.EXTRA_PAN)
        val licenseNo = intent.getStringExtra(MillerDashboardActivity.EXTRA_LICENSE_NO)
        val bank = intent.getStringExtra(MillerDashboardActivity.EXTRA_BANK)
        val branchName = intent.getStringExtra(MillerDashboardActivity.EXTRA_BRANCH_NAME)
        val accountNo = intent.getStringExtra(MillerDashboardActivity.EXTRA_ACCOUNT_NO)
        val ifscCode = intent.getStringExtra(MillerDashboardActivity.EXTRA_IFSC_CODE)
        val millerHeadName = intent.getStringExtra(MillerDashboardActivity.EXTRA_MILLER_HEAD_NAME)
        val mtPerHour = intent.getIntExtra(MillerDashboardActivity.EXTRA_MT_PER_HOUR, 0)

        txtMillerId.text = if (millerId > 0) millerId.toString() else "Data Not Available"
        txtMillerName.text = getDisplayValue(millerName)
        txtDistrict.text = getDisplayValue(districtName)
        txtEmailId.text = getDisplayValue(emailId)
        txtMobileNumber.text = getDisplayValue(mobileNo)
        txtTinNumber.text = getDisplayValue(tinNo)
        txtPan.text = maskPan(pan)
        txtLicenseNumber.text = getDisplayValue(licenseNo)
        txtBank.text = getDisplayValue(bank)
        txtBranch.text = getDisplayValue(branchName)
        txtAccountNumber.text = getDisplayValue(accountNo)
        txtIfscCode.text = getDisplayValue(ifscCode)
        txtMillerHeadName.text = getDisplayValue(millerHeadName)
        txtMillingCapacity.text = if (mtPerHour > 0) "$mtPerHour MT per Hour" else "Data Not Available"
    }

    private fun getDisplayValue(value: String?): String =
        value?.trim()?.takeIf { it.isNotBlank() } ?: "Data Not Available"

    private fun maskPan(pan: String?): String {
        val cleanPan = pan?.trim()
        if (cleanPan.isNullOrBlank()) return "Data Not Available"

        return if (cleanPan.length <= 4) {
            "XX XXXX $cleanPan"
        } else {
            "XX XXXX ${cleanPan.takeLast(4)}"
        }
    }
}
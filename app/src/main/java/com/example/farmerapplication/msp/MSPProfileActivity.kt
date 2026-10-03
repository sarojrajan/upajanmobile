package com.example.farmerapplication.msp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.farmerapplication.R
import com.example.farmerapplication.models.msp.MSPLoginResponse
import com.google.gson.Gson

class MSPProfileActivity : ComponentActivity() {

    private lateinit var txtMspCenterId: TextView
    private lateinit var txtMspCentreName: TextView
    private lateinit var txtEmailId: TextView
    private lateinit var txtDistrict: TextView
    private lateinit var txtManagerName: TextView
    private lateinit var txtManagerAadhaar: TextView
    private lateinit var txtManagerMobile: TextView
    private lateinit var txtAccountNumber: TextView
    private lateinit var txtIfscCode: TextView
    private lateinit var txtBranchName: TextView
    private lateinit var txtBcoName: TextView
    private lateinit var txtBcoMobile: TextView
    private lateinit var txtMspCapacity: TextView

    companion object {
        const val EXTRA_MSP_RESPONSE = "extra_msp_response"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msp_profile_layout)

        initializeViews()
        receiveAndDisplayProfile()
    }

    private fun initializeViews() {
        txtMspCenterId = findViewById(R.id.txtMspCenterId)
        txtMspCentreName = findViewById(R.id.txtMspCentreName)
        txtEmailId = findViewById(R.id.txtEmailId)
        txtDistrict = findViewById(R.id.txtDistrict)
        txtManagerName = findViewById(R.id.txtManagerName)
        txtManagerAadhaar = findViewById(R.id.txtManagerAadhaar)
        txtManagerMobile = findViewById(R.id.txtManagerMobile)
        txtAccountNumber = findViewById(R.id.txtAccountNumber)
        txtIfscCode = findViewById(R.id.txtIfscCode)
        txtBranchName = findViewById(R.id.txtBranchName)
        txtBcoName = findViewById(R.id.txtBcoName)
        txtBcoMobile = findViewById(R.id.txtBcoMobile)
        txtMspCapacity = findViewById(R.id.txtMspCapacity)
    }

    private fun receiveAndDisplayProfile() {
        val responseJson = intent.getStringExtra(EXTRA_MSP_RESPONSE)

        if (responseJson.isNullOrBlank()) {
            Toast.makeText(this, "MSP profile information is unavailable.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            val response = Gson().fromJson(responseJson, MSPLoginResponse::class.java)
            displayProfile(response)
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to load MSP profile.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun displayProfile(response: MSPLoginResponse) {
        txtMspCenterId.text = response.mspCentreId?.toString() ?: "--"
        txtMspCentreName.text = response.mspCentreName?.takeIf { it.isNotBlank() } ?: "--"
        txtEmailId.text = response.emailId?.takeIf { it.isNotBlank() } ?: "--"
        txtDistrict.text = response.districtName?.takeIf { it.isNotBlank() } ?: "--"
        txtManagerName.text = response.managerName?.takeIf { it.isNotBlank() } ?: "--"
        txtManagerAadhaar.text = response.managerAadhaar?.filter { it.isDigit() }?.takeIf { it.length >= 4 }?.let { "XXXX XXXX ${it.takeLast(4)}" } ?: "--"
        txtManagerMobile.text = response.managerMobileNo?.takeIf { it.isNotBlank() } ?: "--"
        txtAccountNumber.text = response.accountNumber?.takeIf { it.isNotBlank() } ?: "--"
        txtIfscCode.text = response.ifscCode?.takeIf { it.isNotBlank() } ?: "--"
        txtBranchName.text = response.branchName?.takeIf { it.isNotBlank() } ?: "--"
        txtBcoName.text = response.bcoName?.takeIf { it.isNotBlank() } ?: "--"
        txtBcoMobile.text = response.bcoMobile?.takeIf { it.isNotBlank() } ?: "--"
        txtMspCapacity.text = response.mspCapacity?.toString() ?: "--"
    }
}
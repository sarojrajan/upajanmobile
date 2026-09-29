package com.example.farmerapplication.msp

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.farmerapplication.R
import android.content.Intent

class MSPReportMainActivity : ComponentActivity() {

    private lateinit var btnScheduledFarmersReport: LinearLayout
    private lateinit var btnFarmersPaddyGivenReport: LinearLayout

    private var mspCentreId: String = ""

    companion object {
        const val EXTRA_MSP_CENTRE_ID = "extra_msp_centre_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msp_report_main_layout)

        initializeViews()
        receiveMSPCentreId()
        setupClickListeners()
    }

    private fun initializeViews() {
        btnScheduledFarmersReport = findViewById(R.id.btnScheduledFarmersReport)
        btnFarmersPaddyGivenReport = findViewById(R.id.btnFarmersPaddyGivenReport)
    }

    private fun receiveMSPCentreId() {
        mspCentreId = intent.getStringExtra(EXTRA_MSP_CENTRE_ID)?.trim().orEmpty()

        if (mspCentreId.isBlank()) {
            Toast.makeText(this, "MSP Centre ID is unavailable.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        btnScheduledFarmersReport.setOnClickListener {
            startActivity(
                Intent(this, ScheduledFarmersReportActivity::class.java)
                    .putExtra(ScheduledFarmersReportActivity.EXTRA_MSP_CENTRE_ID, mspCentreId)
            )
        }
        btnFarmersPaddyGivenReport.setOnClickListener {
            startActivity(
                Intent(this, FarmersPaddyGivenReportActivity::class.java)
                    .putExtra(FarmersPaddyGivenReportActivity.EXTRA_MSP_CENTRE_ID, mspCentreId)
            )
        }
    }

    private fun showFeatureComingSoon() {
        Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
    }
}